import SockJS from 'sockjs-client'
import { Client, type Frame, type StompSubscription } from '@stomp/stompjs'
import type { IMessage } from '@stomp/stompjs'

export interface ChatMessage {
  id?: number
  sessionId: number
  senderId: number
  senderNickname?: string
  content: string
  contentType?: string
  replyToId?: number
  replyToNickname?: string
  replyToContent?: string
  sentAt?: string
}

// 开发环境标识
const isDev = import.meta.env.DEV

// 调试日志函数，仅在开发环境输出
const debugLog = (...args: unknown[]) => {
  if (isDev) {
    console.log(...args)
  }
}

class WebSocketService {
  private client: Client | null = null
  private connected = false
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5

  private currentSessionId: number | null = null
  private messageCallback: ((message: ChatMessage) => void) | null = null
  private currentSubscription: StompSubscription | null = null // 当前订阅对象
  private subscriptions: Map<number, StompSubscription> = new Map() // 所有会话的订阅
  private currentToken: string | null = null // 当前 token
  private currentErrorCallback: ((error: Frame) => void) | null = null // 当前错误回调
  
  // 连接状态变更回调（用于通知 Vue 组件更新 UI）
  private connectionStateCallback: ((connected: boolean) => void) | null = null
  
  /**
   * 设置连接状态变更回调
   */
  onConnectionStateChange(callback: (connected: boolean) => void) {
    this.connectionStateCallback = callback
  }
  
  /**
   * 通知连接状态变更
   */
  private notifyConnectionState() {
    if (this.connectionStateCallback) {
      const isConnected = this.isConnected()
      this.connectionStateCallback(isConnected)
    }
  }

  /**
   * 连接 WebSocket
   */
  connect(token: string, onMessage: (message: ChatMessage) => void, onError?: (error: Frame) => void) {
    // 如果已连接且使用相同的回调，不需要重新连接
    if (this.connected && this.client?.active && this.messageCallback === onMessage) {
      debugLog('WebSocket 已连接，使用现有连接')
      // 更新回调函数
      this.messageCallback = onMessage
      // 如果有当前会话，确保已订阅
      if (this.currentSessionId) {
        this.subscribeToSession(this.currentSessionId, onMessage)
      }
      return
    }

    // 如果已连接但需要重新连接（token 可能变化），先断开
    if (this.connected && this.client?.active) {
      debugLog('WebSocket 已连接，先断开再重新连接以使用新的 token')
      this.disconnect()
      // 等待断开完成
      setTimeout(() => {
        this.doConnect(token, onMessage, onError)
      }, 100)
      return
    }

    this.doConnect(token, onMessage, onError)
  }

  /**
   * 执行实际的连接操作
   */
  private doConnect(token: string, onMessage: (message: ChatMessage) => void, onError?: (error: Frame) => void) {
    console.log('开始执行 WebSocket 连接...')
    this.messageCallback = onMessage
    this.currentToken = token
    this.currentErrorCallback = onError || null

    // 使用 SockJS 作为传输层
    // 开发环境：优先使用 VITE_WS_BASE_URL，未配置时使用本地后端 http://localhost:8080
    // 生产环境：使用 VITE_WS_BASE_URL 或当前站点同源地址
    const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 
      (import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin)
    const wsUrl = `${WS_BASE_URL}/ws`
    console.log('WebSocket URL:', wsUrl)
    const socket = new SockJS(wsUrl)
    
    this.client = new Client({
      webSocketFactory: () => socket as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: {
        'Authorization': `Bearer ${token}`
      },
      onConnect: (frame) => {
        debugLog('WebSocket 连接成功', frame)
        this.connected = true
        this.reconnectAttempts = 0
        
        // 确保客户端状态正确
        if (this.client) {
          debugLog('客户端状态:', {
            active: this.client.active,
            connected: this.connected
          })
        }
        
        // 通知连接状态变更
        this.notifyConnectionState()
        
        // 连接成功后，如果有当前会话，订阅该会话的消息
        if (this.currentSessionId && this.messageCallback) {
          debugLog('WebSocket 连接成功，自动订阅会话:', this.currentSessionId)
          // 使用 setTimeout 确保连接完全建立后再订阅
          setTimeout(() => {
            if (this.currentSessionId && this.messageCallback) {
              this.subscribeToSession(this.currentSessionId, this.messageCallback)
            }
          }, 100)
        } else {
          debugLog('WebSocket 连接成功，但当前没有会话需要订阅', {
            currentSessionId: this.currentSessionId,
            hasCallback: !!this.messageCallback
          })
        }
      },
      onStompError: (frame) => {
        console.error('STOMP 错误:', frame)
        this.connected = false
        this.notifyConnectionState()
        if (onError) {
          onError(frame)
        }
      },
      onWebSocketError: (event) => {
        console.error('WebSocket 错误:', event)
        this.connected = false
        this.notifyConnectionState()
        if (onError) {
          onError(event)
        }
      },
      onWebSocketClose: () => {
        debugLog('WebSocket 连接关闭')
        this.connected = false
        this.notifyConnectionState()
        // 只有在非主动断开的情况下才重连
        if (this.currentToken && this.messageCallback) {
          this.attemptReconnect()
        }
      },
      onDisconnect: () => {
        debugLog('WebSocket 断开连接')
        this.connected = false
        this.notifyConnectionState()
      }
    })

    this.client.activate()
  }

  /**
   * 设置当前会话ID（用于自动订阅）
   */
  setCurrentSessionId(sessionId: number | null) {
    this.currentSessionId = sessionId
    console.log('设置当前会话ID:', sessionId)
  }

  /**
   * 尝试重连
   */
  private attemptReconnect() {
    if (!this.currentToken || !this.messageCallback) {
      debugLog('无法重连：缺少 token 或回调函数')
      return
    }
    
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      debugLog(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
      setTimeout(() => {
        if (this.currentToken && this.messageCallback) {
          this.doConnect(this.currentToken, this.messageCallback, this.currentErrorCallback || undefined)
        }
      }, 5000)
    } else {
      console.error('达到最大重连次数，停止重连')
    }
  }

  /**
   * 发送消息
   */
  sendMessage(sessionId: number, content: string, contentType: string = 'text', replyToId?: number) {
    // 直接检查 client 是否激活，不依赖 isConnected() 方法
    if (!this.client || !this.client.active) {
      console.error('WebSocket 未连接，无法发送消息', {
        connected: this.connected,
        clientActive: this.client?.active,
        hasClient: !!this.client
      })
      return false
    }

    const message: Record<string, unknown> = {
      sessionId,
      content,
      contentType
    }

    // 如果有引用消息，添加引用信息
    if (replyToId) {
      message.replyToId = replyToId
    }

    debugLog('发送消息到 /app/chat.send:', message)
    try {
      this.client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      })
      debugLog('消息已发送')
      return true
    } catch (error) {
      console.error('发送消息失败:', error)
      return false
    }
  }

  /**
   * 订阅特定会话的消息
   */
  subscribeToSession(sessionId: number, onMessage: (message: ChatMessage) => void) {
    // 直接检查 client 是否激活
    if (!this.client || !this.client.active) {
      console.error('WebSocket 未连接，无法订阅会话', {
        connected: this.connected,
        clientActive: this.client?.active,
        hasClient: !!this.client
      })
      return
    }

    // 如果已经订阅了同一个会话，不需要重新订阅
    if (this.currentSessionId === sessionId && this.currentSubscription) {
      debugLog('已订阅该会话，跳过')
      return
    }

    // 取消之前的订阅
    if (this.currentSubscription) {
      debugLog('取消之前的订阅')
      this.currentSubscription.unsubscribe()
      this.currentSubscription = null
    }

    this.currentSessionId = sessionId
    this.messageCallback = onMessage

    const destination = `/topic/session.${sessionId}`
    console.log(`订阅会话消息: ${destination}, 会话ID: ${sessionId}`)
    this.currentSubscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const data: ChatMessage = JSON.parse(message.body)
        debugLog(`收到 WebSocket 消息 [目标: ${destination}, 会话ID: ${sessionId}]:`, {
          raw: message.body,
          parsed: data,
          messageSessionId: data.sessionId,
          currentSessionId: sessionId
        })
        onMessage(data)
      } catch (error) {
        console.error('解析消息失败:', error, '原始消息:', message.body)
      }
    })
    console.log('订阅已创建，等待消息...')
  }

  /**
   * 订阅多个会话
   */
  subscribeToAllSessions(sessionIds: number[], onMessage: (message: ChatMessage) => void) {
    if (!this.client || !this.client.active) {
      console.error('WebSocket 未连接，无法订阅会话')
      return
    }

    // 取消所有之前的订阅
    this.subscriptions.forEach((sub) => sub.unsubscribe())
    this.subscriptions.clear()

    this.messageCallback = onMessage

    // 订阅所有会话
    for (const sessionId of sessionIds) {
      const destination = `/topic/session.${sessionId}`
      debugLog(`订阅会话: ${destination}`)
      const subscription = this.client.subscribe(destination, (message: IMessage) => {
        try {
          const data: ChatMessage = JSON.parse(message.body)
          debugLog(`收到消息 [会话${sessionId}]:`, data)
          onMessage(data)
        } catch (error) {
          console.error('解析消息失败:', error)
        }
      })
      this.subscriptions.set(sessionId, subscription)
    }

    console.log(`已订阅 ${sessionIds.length} 个会话`)
  }

  /**
   * 断开连接
   */
  disconnect() {
    // 取消所有订阅
    this.subscriptions.forEach((sub) => sub.unsubscribe())
    this.subscriptions.clear()

    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe()
      this.currentSubscription = null
    }

    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
    }
    
    // 清除状态，但不清除 currentSessionId（可能还需要）
    this.messageCallback = null
    this.currentToken = null
    this.currentErrorCallback = null
    this.reconnectAttempts = 0
    
    // 通知连接状态变更
    this.notifyConnectionState()
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    // 优先检查 client.active，这是最准确的状态
    // 如果 client 存在且 active，则认为已连接
    if (this.client && this.client.active === true) {
      return true
    }
    // 如果 client 不存在或未激活，检查 connected 标志作为备选
    return this.connected && !!this.client
  }
}

export const wsService = new WebSocketService()

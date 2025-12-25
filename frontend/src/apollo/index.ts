import { ApolloClient, createHttpLink, InMemoryCache } from '@apollo/client/core'

import { setContext } from '@apollo/client/link/context'

import { useUserStore } from '../stores/user' // 引入 user store



// 1. HTTP 连接 (【重要】已修改为 8080 端口)

const httpLink = createHttpLink({

  uri: 'http://localhost:8080/graphql',

})



// 2. 认证中间件 (自动附加 Token)

const authLink = setContext((_, { headers }) => {

  // Pinia store 必须在函数内部获取

  const userStore = useUserStore()

  const token = userStore.token



  return {

    headers: {

      ...headers,

      authorization: token ? `Bearer ${token}` : '', // 将 Token 放入 Header

    }

  }

})



// 3. 创建 Apollo 客户端

export const apolloClient = new ApolloClient({

  link: authLink.concat(httpLink), // 链接认证和 HTTP

  cache: new InMemoryCache(),

})
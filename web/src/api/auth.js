import { request } from './client.js'

/** {alias, email, password} → {token, user} */
export function register(data) {
  return request('/auth/register', { method: 'POST', body: data, auth: false })
}

/** {email, password} → {token, user} */
export function login(data) {
  return request('/auth/login', { method: 'POST', body: data, auth: false })
}

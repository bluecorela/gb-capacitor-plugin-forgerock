# forgerock-bridge

Plugin para comunicarse con el SDK nativos de Forgerock

## Install

```bash
npm install forgerock-bridge
```

## Build

```bash
npm run build
```

## Generate build

```bash
npm run build
```

## API

<docgen-index>

* [`initialize(...)`](#initialize)
* [`authenticate(...)`](#authenticate)
* [`logout()`](#logout)
* [`userInfo()`](#userinfo)
* [`getAccessToken()`](#getaccesstoken)
* [`initializeOTPRegister(...)`](#initializeotpregister)
* [`deleteOTPRegister(...)`](#deleteotpregister)
* [`validateOTP(...)`](#validateotp)
* [`hasRegisteredMechanism()`](#hasregisteredmechanism)
* [`generateOTP()`](#generateotp)
* [`forgotPassword(...)`](#forgotpassword)
* [`getQuestionForgotPassword()`](#getquestionforgotpassword)
* [`answerQuestionForgotPassword(...)`](#answerquestionforgotpassword)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### initialize(...)

```typescript
initialize(options: { url: string; realm: string; journey: string; oauthClientId: string; oauthScope: string; }) => Promise<void>
```

| Param         | Type                                                                                                     |
| ------------- | -------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ url: string; realm: string; journey: string; oauthClientId: string; oauthScope: string; }</code> |

--------------------


### authenticate(...)

```typescript
authenticate(options: { journey: string; username?: string; password?: string; isRetry?: boolean; }) => Promise<{ authId?: string; token?: string; userExists?: boolean; status?: string; errorMessage?: string; callbacks: string[]; }>
```

| Param         | Type                                                                                       |
| ------------- | ------------------------------------------------------------------------------------------ |
| **`options`** | <code>{ journey: string; username?: string; password?: string; isRetry?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ authId?: string; token?: string; userExists?: boolean; status?: string; errorMessage?: string; callbacks: string[]; }&gt;</code>

--------------------


### logout()

```typescript
logout() => Promise<{ message: string; }>
```

**Returns:** <code>Promise&lt;{ message: string; }&gt;</code>

--------------------


### userInfo()

```typescript
userInfo() => Promise<string>
```

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### getAccessToken()

```typescript
getAccessToken() => Promise<string>
```

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### initializeOTPRegister(...)

```typescript
initializeOTPRegister(options: { journey: string; }) => Promise<{ status: string; }>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ journey: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### deleteOTPRegister(...)

```typescript
deleteOTPRegister(options: { journey: string; }) => Promise<{ status: string; }>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ journey: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### validateOTP(...)

```typescript
validateOTP(options: { url: string; }) => Promise<{ empty: boolean; }>
```

| Param         | Type                          |
| ------------- | ----------------------------- |
| **`options`** | <code>{ url: string; }</code> |

**Returns:** <code>Promise&lt;{ empty: boolean; }&gt;</code>

--------------------


### hasRegisteredMechanism()

```typescript
hasRegisteredMechanism() => Promise<{ empty: boolean; }>
```

**Returns:** <code>Promise&lt;{ empty: boolean; }&gt;</code>

--------------------


### generateOTP()

```typescript
generateOTP() => Promise<{ otp: string; }>
```

**Returns:** <code>Promise&lt;{ otp: string; }&gt;</code>

--------------------


### forgotPassword(...)

```typescript
forgotPassword(options: { journey: string; username?: string; }) => Promise<void>
```

| Param         | Type                                                 |
| ------------- | ---------------------------------------------------- |
| **`options`** | <code>{ journey: string; username?: string; }</code> |

--------------------


### getQuestionForgotPassword()

```typescript
getQuestionForgotPassword() => Promise<{ question: string; }>
```

**Returns:** <code>Promise&lt;{ question: string; }&gt;</code>

--------------------


### answerQuestionForgotPassword(...)

```typescript
answerQuestionForgotPassword(options: { answer: string; }) => Promise<void>
```

| Param         | Type                             |
| ------------- | -------------------------------- |
| **`options`** | <code>{ answer: string; }</code> |

--------------------

</docgen-api>

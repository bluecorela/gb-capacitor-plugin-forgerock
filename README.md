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
* [`initForgotPassword(...)`](#initforgotpassword)
* [`getQuestionForgotPassword()`](#getquestionforgotpassword)
* [`answerQuestionForgotPassword(...)`](#answerquestionforgotpassword)
* [`changePasswordForgotPassword(...)`](#changepasswordforgotpassword)
* [`getCurrentSession()`](#getcurrentsession)
* [`isValidAuthMethod(...)`](#isvalidauthmethod)
* [`isValidAuthMethod(...)`](#isvalidauthmethod)
* [`affiliateUser(...)`](#affiliateuser)
* [Type Aliases](#type-aliases)

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


### initForgotPassword(...)

```typescript
initForgotPassword(options: { journey: string; username: string; language: string; }) => Promise<{ status: string; message: string; }>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code>{ journey: string; username: string; language: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; message: string; }&gt;</code>

--------------------


### getQuestionForgotPassword()

```typescript
getQuestionForgotPassword() => Promise<{ question: string; }>
```

**Returns:** <code>Promise&lt;{ question: string; }&gt;</code>

--------------------


### answerQuestionForgotPassword(...)

```typescript
answerQuestionForgotPassword(options: { answer: string; }) => Promise<{ status: string; message: string; }>
```

| Param         | Type                             |
| ------------- | -------------------------------- |
| **`options`** | <code>{ answer: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; message: string; }&gt;</code>

--------------------


### changePasswordForgotPassword(...)

```typescript
changePasswordForgotPassword(options: { password?: string; }) => Promise<{ status: string; message: string; }>
```

| Param         | Type                                |
| ------------- | ----------------------------------- |
| **`options`** | <code>{ password?: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; message: string; }&gt;</code>

--------------------


### getCurrentSession()

```typescript
getCurrentSession() => Promise<{ currentSesion: string; }>
```

**Returns:** <code>Promise&lt;{ currentSesion: string; }&gt;</code>

--------------------


### isValidAuthMethod(...)

```typescript
isValidAuthMethod(options: GetAuthMethodRequest) => Promise<AuthMethodResponse>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#getauthmethodrequest">GetAuthMethodRequest</a></code> |

**Returns:** <code>Promise&lt;<a href="#authmethodresponse">AuthMethodResponse</a>&gt;</code>

--------------------


### isValidAuthMethod(...)

```typescript
isValidAuthMethod(options: ValidAuthMethodRequest) => Promise<AuthMethodResponse>
```

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code><a href="#validauthmethodrequest">ValidAuthMethodRequest</a></code> |

**Returns:** <code>Promise&lt;<a href="#authmethodresponse">AuthMethodResponse</a>&gt;</code>

--------------------


### affiliateUser(...)

```typescript
affiliateUser(options: { journey: string; step: string; meta: string; }) => Promise<{ status: string; message: string; data?: selectOption[]; }>
```

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code>{ journey: string; step: string; meta: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; message: string; data?: selectOption[]; }&gt;</code>

--------------------


### Type Aliases


#### AuthMethodResponse

<code>{ authId?: string; callbacks: PasswordCallback[]; header? : string }</code>


#### PasswordCallback

<code>{ type: "<a href="#passwordcallback">PasswordCallback</a>"; output: [{ name: "prompt"; value: string }]; input: [{ name: "IDToken1"; value: string }]; }</code>


#### GetAuthMethodRequest

<code>{ url: string; trxId: string; }</code>


#### ValidAuthMethodRequest

<code>{ url: string; trxId: string; payload: <a href="#authmethodresponse">AuthMethodResponse</a>; }</code>


#### selectOption

<code>{ label: string; value: string }</code>

</docgen-api>

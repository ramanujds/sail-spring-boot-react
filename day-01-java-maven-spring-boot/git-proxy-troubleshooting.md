# Troubleshooting: `git clone` Fails Behind the Company Proxy

Git talks to the internet on its own. It does **not** read Maven's `settings.xml` or your browser's proxy settings. On a company network, `git clone` can fail even when the browser opens the same address.

This page shows how to recognise the problem and fix it.

## 1. Recognise the problem

Run the clone:

```bash
git clone REPO_URL
```

These messages mean Git cannot reach the server:

```text
fatal: unable to access 'https://github.com/...': Could not resolve host: github.com
```

```text
fatal: unable to access 'https://github.com/...': Failed to connect to github.com port 443 after 21012 ms: Couldn't connect to server
```

```text
fatal: unable to access 'https://github.com/...': Received HTTP code 407 from proxy after CONNECT
```

Each one is a network problem, not a problem with the repository. The steps below find out which one you have.

---

## 2. Step by step

### Step 1: Find your proxy details

Use the same proxy that your browser and Maven use.

| Detail | Example placeholder |
|---|---|
| Proxy host | `PROXY_HOST` |
| Proxy port | `PROXY_PORT` |
| Username and password | Only if your proxy asks for them |

### Step 2: Check that the proxy works outside Git

```bash
curl -I -x http://PROXY_HOST:PROXY_PORT https://github.com
```

If the proxy needs a login, add `-U YOUR_USERNAME:YOUR_PASSWORD`.

**You should see** a line starting with `HTTP/1.1 200` or `HTTP/2 200`.

| Result | Meaning |
|---|---|
| `200` | The proxy works. Go to step 3 |
| `407 Proxy Authentication Required` | The proxy needs a username and password. Go to step 4 |
| Connection refused or timed out | The host or port is wrong, or the proxy is not reachable from your machine |

### Step 3: Tell Git about the proxy

```bash
git config --global http.proxy http://PROXY_HOST:PROXY_PORT
```

This one setting covers both `http://` and `https://` addresses.

> **Warning:** You will find advice online to also set `https.proxy`. Git has no such setting and ignores it. Only `http.proxy` works.

**Check:** run `git config --global --get http.proxy`. **You should see** the address you just set.

### Step 4: Add the login, if the proxy asks for one

```bash
git config --global http.proxy http://YOUR_USERNAME:YOUR_PASSWORD@PROXY_HOST:PROXY_PORT
```

If the password has special characters, replace them with their `%` codes. For example `@` becomes `%40`.

> **Warning:** The password is saved in plain text in `~/.gitconfig` (or `C:\Users\<your-name>\.gitconfig` on Windows). Keep that file in your own user folder. Never copy it into a project.

### Step 5: Try again

First, a quick test that downloads nothing:

```bash
git ls-remote REPO_URL
```

**You should see** a list of lines that each start with a long hexadecimal number and end with a name such as `HEAD` or `refs/heads/main`.

Then clone:

```bash
git clone REPO_URL
```

**You should see** `Cloning into '...'` followed by `Receiving objects: 100%`.

---

## 3. Certificate errors

If the company proxy inspects secure traffic, Git may stop with:

```text
fatal: unable to access 'https://github.com/...': SSL certificate problem: unable to get local issuer certificate
```

This is a **certificate** problem, not a proxy problem. Ask your network team for the company certificate file (a `.pem` or `.crt` file), then tell Git about it:

```bash
git config --global http.sslCAInfo "/path/to/company-ca.pem"
```

> **Warning:** You will find advice online to run `git config --global http.sslVerify false`. Do not. It switches off the security check for every clone.

---

## 4. A proxy for the internet, but not for your own Git server

If your organization runs its own Git server, it is reached directly. Sending it through the proxy fails.

Skip the proxy for that one server by giving it an empty proxy:

```bash
git config --global http.https://GIT_SERVER_HOST.proxy ""
```

The proxy still applies to every other address.

---

## 5. Remove the setting later

```bash
git config --global --unset http.proxy
```

Run `git config --global --get http.proxy` afterwards. It should print nothing.

---

## 6. Common messages

| Message | Cause | Fix |
|---|---|---|
| `Could not resolve host: github.com` | Your machine cannot look up internet names without the proxy | Set the proxy (step 3) |
| `Could not resolve proxy: ...` | The proxy host in the setting has a typo | Run `git config --global --get http.proxy` and compare with the browser's proxy settings |
| `Failed to connect to ... port ...` | The proxy host or port is wrong, or Git is not using the proxy | Check steps 2 and 3 |
| `Received HTTP code 407 from proxy` | Username or password is missing or wrong | Step 4 |
| `SSL certificate problem` | A certificate problem | Section 3 |
| `Authentication failed` after the proxy works | The Git host refused your login. Many hosts no longer accept an account password | Use a personal access token as the password |
| Works with `https://`, fails with `git@...` | An SSH address does not use the HTTP proxy setting | Use the `https://` address of the repository |
| Works in the terminal, fails in IntelliJ | IntelliJ uses its own proxy setting for some operations | Open **Settings → Appearance & Behavior → System Settings → HTTP Proxy** and enter the same host and port |

## Check yourself

- [ ] `curl -I -x http://PROXY_HOST:PROXY_PORT https://github.com` shows `200`.
- [ ] `git config --global --get http.proxy` prints your proxy address.
- [ ] `git ls-remote REPO_URL` prints a list of references.
- [ ] `git clone REPO_URL` ends with `Receiving objects: 100%`.
- [ ] You did not set `sslVerify` to `false`.

## Later in the programme

Git is one of several tools that need the proxy. npm (Days 10 and 11), the server's package manager (Day 8), and calls to `localhost` can also be affected. See [Proxy issues ahead](proxy-issues-ahead.md).

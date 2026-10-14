# Proxy Issues Ahead: Where They Can Appear and How to Solve Them

Yes, the proxy can cause trouble again in the coming days. Maven is already set up. A few other tools and a few other situations need their own setup.

This page tells you **where** to expect a problem, **how to recognise** it, and **how to fix** it. Come back to the right section when you reach that day.

## 1. Three things called "proxy"

The word "proxy" means three different things in this programme. Only the first one is a problem to fix.

| Name | What it is | Where you meet it | Your job |
|---|---|---|---|
| **Company proxy** | A server your company puts between your machine and the internet | Maven, npm, Git, your browser | Tell each tool about it (this page) |
| **Reverse proxy** | nginx, in front of your application, forwarding `/api/**` to it | Days 8 and 12 | Nothing to fix. You set it up on purpose |
| **Dev-server proxy** | A setting in the React dev server that forwards `/api` calls to your backend | Days 10 and 11, if used | Nothing to fix. It is part of the project |

If you see an error, first work out which of the three it is. The rest of this page is about the company proxy.

## 2. Where to expect it

| Day | What goes online | Proxy problem? | Go to |
|---|---|---|---|
| **2 to 7** | New Maven libraries (JPA, H2, validation, security, test libraries, Oracle driver) | Already solved by your `settings.xml` | [Maven proxy troubleshooting](maven-proxy-troubleshooting.md) |
| **2 to 7** | Calls to `localhost` (H2 console, `curl`, Postman, browser) | **Possible.** The proxy can get in the way of local calls | [Section 3](#3-calls-to-localhost-go-through-the-proxy) |
| **7** | Oracle database connection | **No.** It is not an HTTP connection, so the proxy is not involved | [Section 4](#4-oracle-is-not-a-proxy-problem) |
| **8** | Installing nginx on the server, Git on the server | **Possible.** The server's package manager needs the proxy | [Section 5](#5-day-8-the-server) |
| **9** | Bootstrap from a CDN link | **Possible.** The CDN may be blocked | [Section 6](#6-day-9-bootstrap-and-cdn-links) |
| **10 and 11** | Node.js, npm packages, Vite, React Router, Axios | **Yes.** npm has its own proxy setting | [Section 7](#7-days-10-and-11-npm) |
| **12** | Opening the internal hostname in a browser | **Possible.** The browser may send it to the proxy | [Section 8](#8-day-12-internal-hostnames) |
| **Any day** | `git clone`, `git pull`, `git push` over `https` | **Possible.** Git has its own proxy setting | [Section 9](#9-git) |

---

## 3. Calls to `localhost` go through the proxy

**Days 2 to 12.**

Your application runs on your own machine at `localhost:8080`. A request to it should never leave your machine. If your tool is set to use the company proxy for everything, it sends the request to the proxy, and the proxy cannot reach your machine.

**What you see**

- `curl` returns nothing, an error, or a page from the proxy instead of your data.
- The browser shows a proxy error page, or says the proxy server refused the connection.
- Postman shows `502`, `503`, or a message mentioning the proxy.
- The same URL works in one tool and not in another.

**Fix:** tell the tool to skip the proxy for local addresses.

| Tool | How |
|---|---|
| `curl` (one command) | `curl --noproxy localhost http://localhost:8080/tickets` |
| `curl` (every command in this terminal) | Linux and macOS: `export NO_PROXY=localhost,127.0.0.1`. Windows: `set NO_PROXY=localhost,127.0.0.1` |
| Browser | In the browser's proxy settings, add `localhost` and `127.0.0.1` to the list of addresses that do not use the proxy |
| Postman | Open **Settings → Proxy** and add `localhost` and `127.0.0.1` to the bypass list |
| IntelliJ | Open **Settings → Appearance & Behavior → System Settings → HTTP Proxy** and add `localhost, 127.0.0.1` to **No proxy for** |

**Check:** `curl http://localhost:8080/hello` prints your text, and the same URL works in the browser.

> **Note:** The H2 console (`http://localhost:8080/h2-console`) is a web page, so it needs the browser fix.

---

## 4. Oracle is not a proxy problem

**Day 7.**

A company proxy forwards **web** traffic (HTTP and HTTPS). Your application talks to Oracle with a direct connection to the database host and port (usually `1521`). That connection does not use the proxy, so adding proxy settings will not help.

If the connection fails, check these instead:

| Message | Likely cause | Fix |
|---|---|---|
| `Connection refused` | Wrong host or port, or the database is not running | Check the URL in `application-oracle.yml` against what your database team gave you |
| `Connection timed out` | A firewall or network route blocks the port | Check you are on the right network or VPN, then ask your network team to open the port |
| `ORA-01017: invalid username/password` | Wrong login | Check the username and password. Keep them in environment variables, not in the file |
| `ORA-12514` or `ORA-12505` | Wrong service name or SID | Check the service name with your database team |

The Oracle driver (`ojdbc`) is downloaded by Maven from Maven Central like every other library, so your Maven proxy setting already covers it.

---

## 5. Day 8: the server

**Day 8.**

Your server may not reach the internet directly either. These steps need it:

| Step | Needs the proxy? |
|---|---|
| Copying the `.jar` from your machine to the server | No. It uses `scp` or a file share on your network |
| Installing nginx with the package manager | **Yes** |
| Running the application as a service | No. The application does not call out to the internet |
| Checking `/actuator/health` from another machine | No, unless that machine's browser or `curl` proxies local addresses (section 3) |

**Build on your own machine, not on the server.** Then the server does not need Maven or Java build tools, and it needs no Maven proxy settings.

**Installing nginx behind the proxy**

| Server system | Where to add the proxy |
|---|---|
| Ubuntu or Debian (`apt`) | Create `/etc/apt/apt.conf.d/95proxy` containing `Acquire::http::Proxy "http://PROXY_HOST:PROXY_PORT/";` and `Acquire::https::Proxy "http://PROXY_HOST:PROXY_PORT/";` |
| RHEL, Rocky, or Fedora (`dnf`) | Add `proxy=http://PROXY_HOST:PROXY_PORT` to `/etc/dnf/dnf.conf` |

If the proxy needs a login, `dnf` also accepts `proxy_username=` and `proxy_password=` in the same file.

> **Note:** A systemd service does not read the proxy variables from your shell. This is fine, because the application does not need them.

---

## 6. Day 9: Bootstrap and CDN links

**Day 9.**

Most Bootstrap examples on the internet load the files from a public CDN:

```html
<link rel="stylesheet" href="https://cdn.example.com/bootstrap.min.css">
```

On a company network, this link can be blocked. Your page then loads, but with **no styling**: plain text, no buttons, no table borders.

**Fix:** keep the files on your own machine.

1. Download `bootstrap.min.css` and `bootstrap.bundle.min.js` once (from the Bootstrap website, or with `npm install bootstrap` and copy them from `node_modules/bootstrap/dist/`).
2. Put them in your project, for example in a `css` and a `js` folder.
3. Link to them with a local path:

```html
<link rel="stylesheet" href="css/bootstrap.min.css">
<script src="js/bootstrap.bundle.min.js"></script>
```

This also fits the rule of the programme: the finished application does not depend on anything outside your network.

> **Note:** If your page loads but the `fetch` call fails with a **CORS** error in the browser console, that is not a proxy problem. It means the browser blocks a call from one address (for example `localhost:5500`) to another (`localhost:8080`). The fix is a CORS setting in the backend, which you do on Day 9.

---

## 7. Days 10 and 11: npm

**Days 10 and 11.** This is the biggest one.

React needs **Node.js** and **npm**. npm downloads packages from the npm registry, and it does **not** read Maven's `settings.xml`. It has its own proxy setting.

**What you see** when npm cannot reach the registry:

```text
npm error code ECONNREFUSED
npm error FetchError: request to https://registry.npmjs.org/react failed, reason: connect ECONNREFUSED ...
```

Other messages that mean the same thing: `ETIMEDOUT`, `ENOTFOUND registry.npmjs.org`, `ECONNRESET`, and `407 Proxy Authentication Required`.

**Fix**

1. Tell npm about the proxy (use your own host and port):

   ```bash
   npm config set proxy http://PROXY_HOST:PROXY_PORT
   npm config set https-proxy http://PROXY_HOST:PROXY_PORT
   ```

   > **Note:** Both values start with `http://`, even `https-proxy`. The `https` part names the kind of traffic, not the proxy's own address.

2. If the proxy asks for a login, include it in the address:

   ```bash
   npm config set proxy http://YOUR_USERNAME:YOUR_PASSWORD@PROXY_HOST:PROXY_PORT
   npm config set https-proxy http://YOUR_USERNAME:YOUR_PASSWORD@PROXY_HOST:PROXY_PORT
   ```

   If the password has special characters, replace them with their `%` codes. For example `@` becomes `%40`.

3. Check what npm has stored:

   ```bash
   npm config get proxy
   npm config get https-proxy
   ```

4. Test it:

   ```bash
   npm view react version
   ```

   **You should see** a version number, for example `19.1.0`.

**Where the settings go:** into the `.npmrc` file in your user folder (`~/.npmrc`, or `C:\Users\<your-name>\.npmrc` on Windows). Keep the proxy there. Never put it in a `.npmrc` inside a project, because that file is committed to Git.

**To remove them later**

```bash
npm config delete proxy
npm config delete https-proxy
```

### Certificate errors

If the company proxy inspects secure traffic, npm may stop with one of these:

```text
UNABLE_TO_VERIFY_LEAF_SIGNATURE
SELF_SIGNED_CERT_IN_CHAIN
UNABLE_TO_GET_ISSUER_CERT_LOCALLY
```

These are **certificate** problems, not proxy problems. Ask your network team for the company certificate file (a `.pem` or `.crt` file) and tell npm about it:

```bash
npm config set cafile "/path/to/company-ca.pem"
```

> **Warning:** You will find advice online to run `npm config set strict-ssl false`. Do not. It switches off the security check for every download.

### An internal registry

Some organizations run their own npm registry. If yours does, you will be given its address:

```bash
npm config set registry REGISTRY_URL
```

---

## 8. Day 12: internal hostnames

**Day 12.**

On Day 12 you open the application at an internal address such as `helpdesk.internal.company.lan`. If your browser sends **every** address to the company proxy, the proxy looks for that name on the internet, does not find it, and shows an error page.

**What you see:** a proxy error such as "cannot resolve host", "site cannot be reached", or a page from the proxy product. `ping` and `curl --noproxy` to the same name work.

**Fix:** add the internal domain to the browser's list of addresses that skip the proxy, for example `*.company.lan`. Ask your network team for the correct domain. On Windows and macOS, this list is in the system proxy settings, and the browser reads it from there.

**Check:** the address opens the HelpDesk Pro login page from your own machine and from a second machine on the network.

> **Note:** The nginx server from Days 8 and 12 is a **reverse proxy**. It is not the company proxy. Nothing in this section changes it.

---

## 9. Git

**Any day.**

If `git clone`, `git pull`, or `git push` over `https` fails with `Could not resolve host`, `Failed to connect`, or `407`, tell Git about the proxy:

```bash
git config --global http.proxy http://PROXY_HOST:PROXY_PORT
```

This one setting covers both `http://` and `https://` addresses. Git has no `https.proxy` setting.

To remove it:

```bash
git config --global --unset http.proxy
```

For the full steps, a test command, certificate errors and a table of messages, see [Git proxy troubleshooting](git-proxy-troubleshooting.md).

---

## 10. If the proxy keeps getting in the way

| Option | When to use it |
|---|---|
| **Internal Maven mirror** | Your organization runs its own library store. Ask for its address, then add it to `settings.xml` inside `<settings>`: `<mirrors><mirror><id>company-mirror</id><mirrorOf>*</mirrorOf><url>MIRROR_URL</url></mirror></mirrors>` |
| **Work offline** | After one successful online build, everything Maven needs is in your `.m2` folder. In IntelliJ tick **Work offline** in **Settings → Build Tools → Maven**, or run `mvn -o clean package` |
| **Share a library folder** | A machine where the build worked can copy its `.m2/repository` folder to yours |

> **Note:** Offline mode only works for libraries you have already downloaded. A new dependency still needs the network once.

---

## 11. What you see, and where to look

| What you see | It is | Go to |
|---|---|---|
| `Could not transfer artifact` | Maven cannot reach the internet | [Maven proxy troubleshooting](maven-proxy-troubleshooting.md) |
| Build fails on `apache-maven-...-bin.zip` | IntelliJ is using the Maven wrapper | [IntelliJ Maven home path](intellij-maven-home-path.md) |
| `Could not resolve host` or `Received HTTP code 407` from `git clone` | Git cannot reach the server through the proxy | [Git proxy troubleshooting](git-proxy-troubleshooting.md) |
| `npm error ... ECONNREFUSED`, `ETIMEDOUT`, `ENOTFOUND` | npm cannot reach the registry | [Section 7](#7-days-10-and-11-npm) |
| `SELF_SIGNED_CERT_IN_CHAIN` or similar | A certificate problem | [Section 7](#certificate-errors) |
| A proxy error page for `localhost` | The proxy is catching local calls | [Section 3](#3-calls-to-localhost-go-through-the-proxy) |
| Page loads with no styling | A CDN link is blocked | [Section 6](#6-day-9-bootstrap-and-cdn-links) |
| `Connection refused` to the database | Not a proxy problem | [Section 4](#4-oracle-is-not-a-proxy-problem) |
| Proxy error for the internal hostname | The browser sends it to the proxy | [Section 8](#8-day-12-internal-hostnames) |
| CORS error in the browser console | Not a proxy problem. A backend setting | [Section 6](#6-day-9-bootstrap-and-cdn-links) |

## Check yourself

- [ ] `curl --noproxy localhost http://localhost:8080/hello` works, and so does the plain `curl` without the flag.
- [ ] Your browser can open `http://localhost:8080/hello`.
- [ ] You know that Oracle connections do not use the company proxy.
- [ ] You know the two npm commands that set the proxy, and where the setting is saved.
- [ ] You can tell the company proxy from the nginx reverse proxy.

/* Shared fetch wrapper: primes/echoes the CSRF cookie, parses the
   ApiResponse<T> envelope every backend endpoint returns, and throws
   a plain Error with the backend's own message on failure so callers
   can show it directly. */

const Api = (() => {
  function getCookie(name) {
    const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
    return match ? decodeURIComponent(match[2]) : null;
  }

  async function primeCsrf() {
    if (!getCookie("XSRF-TOKEN")) {
      await fetch("/api/csrf", { credentials: "same-origin" });
    }
  }

  async function request(path, { method = "GET", json, form, headers = {} } = {}) {
    if (method !== "GET") {
      await primeCsrf();
      headers["X-XSRF-TOKEN"] = getCookie("XSRF-TOKEN");
    }

    const init = { method, credentials: "same-origin", headers };
    if (json !== undefined) {
      init.headers["Content-Type"] = "application/json";
      init.body = JSON.stringify(json);
    } else if (form !== undefined) {
      init.body = form; // FormData -- browser sets the multipart boundary itself
    }

    const response = await fetch(path, init);
    let body = null;
    try {
      body = await response.json();
    } catch (e) {
      // empty body (e.g. logout) -- fine
    }

    if (!response.ok || (body && body.success === false)) {
      const message = (body && body.message) || `Request failed (${response.status})`;
      throw new Error(message);
    }
    return body ? body.data : null;
  }

  return {
    get: (path) => request(path),
    post: (path, json) => request(path, { method: "POST", json }),
    put: (path, json) => request(path, { method: "PUT", json }),
    del: (path) => request(path, { method: "DELETE" }),
    postForm: (path, form) => request(path, { method: "POST", form }),
    putForm: (path, form) => request(path, { method: "PUT", form }),
    primeCsrf,
  };
})();

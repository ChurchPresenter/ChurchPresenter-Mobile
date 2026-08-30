package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

private const val TAG = "FramingCheck"

/**
 * Whether a site refuses to be shown inside another page.
 *
 * The browser screen shows a web page in an iframe, and a great many sites —
 * Google among them — send a header telling browsers not to allow that. The
 * browser obeys it and paints its own error page, which then goes on the
 * audience screen. Nothing in this app can override that: the rule is enforced
 * by the browser on the site's behalf, and the only way around it is to proxy
 * and rewrite the page, which breaks scripts, cookies and sign-in on anything
 * real.
 *
 * What the app *can* do is find out first and say so, while the operator is
 * still looking at the phone.
 *
 * A screen attached to this phone is unaffected: it loads the address as a page
 * in its own right rather than inside another one, so the header does not apply.
 */
object FramingCheck {

    /**
     * Reads the two headers that govern framing.
     *
     * Deliberately a heuristic, and deliberately cautious in only one direction:
     * it reports a refusal only when a header plainly says so, because a false
     * warning about a site that would have worked is worse than none — the
     * operator can always try it and look at the screen.
     *
     * @param xFrameOptions The `X-Frame-Options` header, if the site sent one.
     * @param csp The `Content-Security-Policy` header, if the site sent one.
     */
    internal fun refusesFramingFrom(xFrameOptions: String?, csp: String?): Boolean {
        val xfo = xFrameOptions?.trim()?.lowercase()
        if (xfo == "deny" || xfo == "sameorigin") return true

        val ancestors = csp?.lowercase()
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("frame-ancestors") }
            ?: return false
        val values = ancestors.removePrefix("frame-ancestors").trim()
        // A wildcard lets anyone frame it; 'none' lets nobody. Anything else is a
        // list of origins, and this phone's address on a hall network will not be
        // on it.
        if (values.contains("*")) return false
        return true
    }

    /**
     * Asks [url] whether it can be framed. False when the question cannot be
     * answered — an unreachable site is a different problem, and one the
     * operator will see for themselves the moment they project it.
     */
    suspend fun refusesFraming(url: String, client: HttpClient): Boolean = apiRunCatching {
        val response: HttpResponse = client.get(url)
        val refuses = refusesFramingFrom(
            xFrameOptions = response.headers["X-Frame-Options"],
            csp = response.headers["Content-Security-Policy"],
        )
        Logger.d(TAG, "refusesFraming($url) = $refuses")
        refuses
    }.getOrDefault(false)
}

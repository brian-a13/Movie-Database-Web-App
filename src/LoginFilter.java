import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;



/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();
    private final ArrayList<String> adminURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        String username = httpRequest.getParameter("username");
        String password = httpRequest.getParameter("password");

        try {
            String token = httpRequest.getHeader("Authorization");

            // Validate the token (e.g., check against a list of valid tokens, verify expiration, etc.)
            if (token.equals("Bearer nuncaSerMadridsta10")) {
                // Token is valid, allow the request to proceed
                chain.doFilter(request, response);
                return;
            }
        }catch (Exception e){
            System.out.println(e);
        }

        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI())) {
            // Keep default action: pass along the filter chain
            chain.doFilter(request, response);
            return;
        }
        if (httpRequest.getSession().getAttribute("user") == null) {
            // Redirect to login page if the "user" attribute doesn't exist in session
            if (httpRequest.getRequestURI().toLowerCase().endsWith("_dashboard")) {
                httpResponse.sendRedirect("_dashboard/login.html");
            }
            else {
                httpResponse.sendRedirect("login.html");
            }
        } else if (!((User) httpRequest.getSession().getAttribute("user")).isAdmin() &&
                    isUrlOnlyForAdmin(httpRequest.getRequestURI())) {
            // user is NOT an admin and the url they requested is only for admins
            if (httpRequest.getRequestURI().toLowerCase().endsWith("_dashboard")) {
                httpResponse.sendRedirect("_dashboard/login.html");
            }
            else {
                httpResponse.sendRedirect("login.html");
            }
        }
        else {
            // user (admin or not) requested a non-admin url
            if (httpRequest.getRequestURI().toLowerCase().endsWith("_dashboard")) {
                httpResponse.sendRedirect("_dashboard/metadata.html");
            }
            else {
                chain.doFilter(request, response);
            }
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    private boolean isUrlOnlyForAdmin(String requestURI) {
        return adminURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");  // works for both regular user / admin pages
        allowedURIs.add("login.js");    // same for this
        allowedURIs.add("api/login");
        allowedURIs.add("global.css");
        allowedURIs.add("_dashboard/api/admin-login");
        allowedURIs.add("_dashboard/login.html");

        adminURIs.add("_dashboard/metadata.html");
        adminURIs.add("_dashboard/new-star.html");
        adminURIs.add("_dashboard/new-movie.html");
        adminURIs.add("_dashboard");
    }

    public void destroy() {
        // ignored.
    }

}

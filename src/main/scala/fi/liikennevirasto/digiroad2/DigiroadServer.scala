package fi.liikennevirasto.digiroad2

import java.util.Properties
import java.util.logging.Logger
import javax.servlet.Servlet
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

import org.eclipse.jetty.client.{HttpClient, HttpProxy, ProxyConfiguration}

import scala.collection.JavaConversions._
import org.eclipse.jetty.http.{MimeTypes, HttpURI}
import org.eclipse.jetty.proxy.ProxyServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.client.api.Request


trait DigiroadServer {
  val contextPath : String

  def startServer() {
    val server = new Server(8080)
    val context = new WebAppContext()
    context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
    context.setResourceBase("src/main/webapp")
    context.setContextPath(contextPath)
    context.setParentLoaderPriority(true)
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    context.addServlet(classOf[NLSProxyServlet], "/maasto/*")
    context.addServlet(classOf[VKMProxyServlet], "/vkm/*")
    context.addServlet(classOf[VKMUIProxyServlet], "/viitekehysmuunnin/*")
    context.addServlet(classOf[MonitoringServlet], "/monitor")
    context.getMimeTypes.addMimeMapping("ttf", "application/x-font-ttf")
    context.getMimeTypes.addMimeMapping("woff", "application/x-font-woff")
    context.getMimeTypes.addMimeMapping("eot", "application/vnd.ms-fontobject")
    context.getMimeTypes.addMimeMapping("js", "application/javascript; charset=UTF-8")
    server.setHandler(context)
    server.start()
    server.join()
  }
}

class NLSProxyServlet extends ProxyServlet {
  override def rewriteURI(req: HttpServletRequest): java.net.URI = {
    val uri = req.getRequestURI
    java.net.URI.create("http://karttamoottori.maanmittauslaitos.fi"
      + uri.replaceFirst("/digiroad", ""))
  }

  override def sendProxyRequest(clientRequest: HttpServletRequest, proxyResponse: HttpServletResponse, proxyRequest: Request): Unit = {
    proxyRequest.header("Referer", "http://www.paikkatietoikkuna.fi/web/fi/kartta")
    proxyRequest.header("Host", null)
    super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest)
  }

  override def getHttpClient: HttpClient = {
    val client = super.getHttpClient
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream("/digiroad2.properties"))
    if (properties.getProperty("http.proxySet", "false").toBoolean) {
      val proxy = new HttpProxy(properties.getProperty("http.proxyHost", "localhost"), properties.getProperty("http.proxyPort", "80").toInt)
      proxy.getExcludedAddresses.addAll(properties.getProperty("http.nonProxyHosts", "").split("|").toList)
      client.getProxyConfiguration.getProxies.add(proxy)
      client.setIdleTimeout(60000)
    }
    client
  }
}

class VKMProxyServlet extends ProxyServlet {
  override def rewriteURI(req: HttpServletRequest): java.net.URI = {
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream("/digiroad2.properties"))
    val vkmUrl: String = properties.getProperty("digiroad2.VKMUrl")
    java.net.URI.create(vkmUrl + req.getRequestURI.replaceFirst("/digiroad", ""))
  }

  override def sendProxyRequest(clientRequest: HttpServletRequest, proxyResponse: HttpServletResponse, proxyRequest: Request): Unit = {
    val parameters = clientRequest.getParameterMap
    parameters.foreach { case(key, value) =>
      proxyRequest.param(key, value.mkString(""))
    }
    super.sendProxyRequest(clientRequest, proxyResponse, proxyRequest)
  }
}

class VKMUIProxyServlet extends ProxyServlet {
  override def rewriteURI(req: HttpServletRequest): java.net.URI = {
    java.net.URI.create("http://localhost:3000" + req.getRequestURI.replaceFirst("/digiroad/viitekehysmuunnin/", "/"))
  }
}

class MonitoringServlet extends HttpServlet {
  override def doGet(req : HttpServletRequest, resp : HttpServletResponse) = {
    val properties = new Properties()
    properties.load(getClass.getResourceAsStream("/digiroad2.properties"))
    val role = properties.getProperty("digiroad2.server.role", "unknown")
    resp.setContentType("text/plain")
    if ("master".equalsIgnoreCase(role)) {
      resp.setStatus(200)
      resp.getWriter.write("OK")
    } else if ("standby".equalsIgnoreCase(role)) {
      handleStandby(resp, properties)
    } else {
      log("Unspecified server role, returning error")
      resp.setStatus(500)
      resp.getWriter.write("No role specified!")
    }
  }

  def handleStandby(resp : HttpServletResponse, properties : Properties) = {
    val host = properties.getProperty("digiroad2.server.master", "")
    if (host.isEmpty) {
      log("Unspecified master host, returning error")
      resp.getWriter.write("No master specified!")
      resp.setStatus(500)
    } else {
      val client = new HttpClient()
      client.setConnectTimeout(2000)
      if (client.GET(host).getContentAsString.equals("OK")) {
        resp.setStatus(503)
        resp.getWriter.write("Master is alive")
      } else {
        resp.setStatus(200)
        resp.getWriter.write("OK")
      }
    }
  }
}

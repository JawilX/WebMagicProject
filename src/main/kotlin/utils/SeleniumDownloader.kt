import org.apache.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Request
import us.codecraft.webmagic.Task
import us.codecraft.webmagic.downloader.Downloader
import us.codecraft.webmagic.selector.Html
import us.codecraft.webmagic.selector.PlainText
import java.io.Closeable
import java.io.IOException
import kotlin.collections.Map.Entry

/**
 * @param chromeDriverPath 使用自己chromedriver可执行文件的完整路径
 */
class SeleniumDownloader(chromeDriverPath: String = "/Users/xu/Crawlers/WebMagicProject/chromedriver") : Downloader, Closeable {
    @Volatile private var webDriverPool: WebDriverPool? = null
    private val logger = Logger.getLogger(this.javaClass)
    private var sleepTime = 0
    private var poolSize = 1

    init {
        System.getProperties().setProperty("webdriver.chrome.driver", chromeDriverPath)
    }

    fun setSleepTime(sleepTime: Int): SeleniumDownloader {
        this.sleepTime = sleepTime
        return this
    }

    override fun download(request: Request, task: Task): Page? {
        this.checkInit()

        val webDriver: WebDriver
        try {
            webDriver = this.webDriverPool!!.get()
        } catch (var10: InterruptedException) {
            this.logger.warn("interrupted", var10)
            return null
        }

        this.logger.info("downloading page " + request.url)
        webDriver.get(request.url)

        try {
            Thread.sleep(this.sleepTime.toLong())
        } catch (var9: InterruptedException) {
            var9.printStackTrace()
        }

        val manage = webDriver.manage()
        val site = task.site
        if (site.cookies != null) {
            val var6 = site.cookies.entries.iterator()

            while (var6.hasNext()) {
                val cookieEntry = var6.next() as Entry<*, *>
                val cookie = Cookie(cookieEntry.key as String, cookieEntry.value as String)
                manage.addCookie(cookie)
            }
        }

        val webElement = webDriver.findElement(By.xpath("/html"))
        val content = webElement.getAttribute("outerHTML")
        val page = Page()
        page.rawText = content
        page.html = Html(content, request.url)
        page.url = PlainText(request.url)
        page.request = request
        this.webDriverPool!!.returnToPool(webDriver)
        return page
    }

    private fun checkInit() {
        if (this.webDriverPool == null) {
            synchronized(this) {
                this.webDriverPool = WebDriverPool(this.poolSize)
            }
        }

    }

    override fun setThread(thread: Int) {
        this.poolSize = thread
    }

    @Throws(IOException::class)
    override fun close() {
        this.webDriverPool!!.closeAll()
    }

}

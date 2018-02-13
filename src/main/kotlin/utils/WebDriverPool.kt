import org.apache.log4j.Logger
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicInteger

internal class WebDriverPool @JvmOverloads constructor(private val capacity: Int = 5) {
    private val logger: Logger
    private val stat: AtomicInteger
    private var mDriver: WebDriver? = null
    private val mAutoQuitDriver: Boolean
    private val webDriverList: MutableList<WebDriver>
    private val innerQueue: BlockingDeque<WebDriver>

    @Throws(IOException::class)
    fun configure() {
        sCaps = DesiredCapabilities.chrome()
        sCaps?.isJavascriptEnabled = true
        sCaps?.setCapability("takesScreenshot", false)
        // https://sites.google.com/a/chromium.org/chromedriver/capabilities
        val chromeOptions = ChromeOptions()
        // 设置不加载图片
        chromeOptions.setExperimentalOption("prefs", hashMapOf(Pair("profile.default_content_setting_values.images", 2)))
        sCaps?.setCapability(ChromeOptions.CAPABILITY, chromeOptions)

        /*val cliArgsCap = ArrayList<Any>()
        cliArgsCap.add("--web-security=false")
        cliArgsCap.add("--ssl-protocol=any")
        cliArgsCap.add("--ignore-ssl-errors=true")*/

        this.mDriver = ChromeDriver(sCaps)

    }

    private fun isUrl(urlString: String): Boolean {
        try {
            URL(urlString)
            return true
        } catch (e: MalformedURLException) {
            return false
        }

    }

    init {
        this.logger = Logger.getLogger(this.javaClass)
        this.stat = AtomicInteger(1)
        this.mDriver = null
        this.mAutoQuitDriver = true
        this.webDriverList = Collections.synchronizedList<WebDriver>(ArrayList())
        this.innerQueue = LinkedBlockingDeque()
    }

    @Throws(InterruptedException::class)
    fun get(): WebDriver {
        this.checkRunning()
        val poll = this.innerQueue.poll()
        if (poll != null) {
            return poll
        } else {
            if (this.webDriverList.size < this.capacity) {
                synchronized(this.webDriverList) {
                    if (this.webDriverList.size < this.capacity) {
                        try {
                            this.configure()
                            this.innerQueue.add(this.mDriver)
                            this.webDriverList.add(this.mDriver!!)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                }
            }

            return this.innerQueue.take()
        }
    }

    fun returnToPool(webDriver: WebDriver) {
        this.checkRunning()
        this.innerQueue.add(webDriver)
    }

    protected fun checkRunning() {
        if (!this.stat.compareAndSet(1, 1)) {
            throw IllegalStateException("Already closed!")
        }
    }

    fun closeAll() {
        val b = this.stat.compareAndSet(1, 2)
        if (!b) {
            throw IllegalStateException("Already closed!")
        } else {
            var webDriver: WebDriver?
            val iterator = this.webDriverList.iterator()
            while (iterator.hasNext()) {
                webDriver = iterator.next()
                this.logger.info("Quit webDriver" + webDriver)
                webDriver.quit()
            }

        }
    }

    companion object {
        private val DEFAULT_CAPACITY = 5
        private val STAT_RUNNING = 1
        private val STAT_CLODED = 2
        private val DRIVER_FIREFOX = "firefox"
        private val DRIVER_CHROME = "chrome"
        protected var sConfig: Properties? = null
        protected var sCaps: DesiredCapabilities? = null
    }
}

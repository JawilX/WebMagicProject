import qidian.Novel
import qidian.NovelDao
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Site
import us.codecraft.webmagic.Spider
import us.codecraft.webmagic.downloader.PhantomJSDownloader
import us.codecraft.webmagic.processor.PageProcessor


class QiDianPageProcessor : PageProcessor {
    val https = "https:"

    private val site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setCharset("UTF-8")
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")

    override fun getSite(): Site {
        return site
    }

    override fun process(page: Page) {
        // 数据库已经有的就不再抓取
        if (urlAndScoreMap.get(page.url.toString()) != null) {
            return
        }
        // 完本小说列表页
        if (page.url.regex(".qidian.com/finish*").match()) {
            // 小说的链接
            val urls = page.html.xpath("//div[@class='book-img-box']/a/@href").all().map { https + it }

            // 页面的链接
            val pageUrls = page.html.xpath("//a[@class='lbf-pagination-page']/@href")
                    .all()
                    .map {
                        https + it
                    }
            page.addTargetRequests(pageUrls)

            urls.filter { urlAndScoreMap.get(it) == null }
                    .forEach { page.addTargetRequest(it) }
        }

        // 小说详情页
        if (page.url.regex(".book.qidian.com/info.*").match()) {

            val name = page.html.xpath("//div[@class='book-info']/h1/em/text()").get()
            val author = page.html.xpath("//div[@class='book-info']/h1/span/a/text()").get()
            val wordCount = (page.html.xpath("//div[@class='book-info']/p[3]/em/text()").get()
                    + page.html.xpath("//div[@class='book-info']/p[3]/cite/text()").get())
            val score = (page.html.xpath("//*[@id='score1']/text()").get()
                    + "."
                    + page.html.xpath("//*[@id='score2']/text()").get())
            val scoreCount = page.html.xpath("//*[@id='j_userCount']/span/text()").get()
            val intro = page.html.xpath("//div[@class='book-intro']/p/text()").get()
            try {
                val novel = Novel(
                        name,
                        author,
                        wordCount,
                        score,
                        if (scoreCount ?: "" == "") "0" else scoreCount,
                        page.url.toString(),
                        intro)
//            page.putField("Novel", novel)
                NovelDao.instance.insert(novel)
                NovelDao.instance.update(novel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private lateinit var urlAndScoreMap: HashMap<String, String>

        @JvmStatic
        fun main(args: Array<String>) {
            urlAndScoreMap = NovelDao.instance.selectUrlAndScore()

            val phantomJSDownloader = PhantomJSDownloader(
                    "phantomjs",
                    "/Users/xu/Crawlers/WebMagicProject/src/main/resources/crawl.js") // gradle打包方式需要自己指定路径
                    .setRetryNum(3)

            Spider.create(QiDianPageProcessor())
                    .addUrl("https://www.qidian.com/finish")
//                    .setDownloader(phantomJSDownloader)
                    .setDownloader(SeleniumDownloader())
//                    .addPipeline(ConsolePipeline())
//                    .thread((Runtime.getRuntime().availableProcessors() - 1) shl 1)
                    .thread(2)
                    .runAsync()

        }
    }

}
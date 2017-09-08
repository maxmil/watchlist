
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Test

class WatchlistProcessorTest {

    val store = BlockingEventStore()
    val watchlist = Watchlist(store)
    val watchlistProcessor = WatchlistProcessor(store, watchlist)

    @Test
    fun add_system_wide_stock() {
        watchlistProcessor.process(AddStock("VOD.L"))

        watchlist.catchup()

        assertThat(store, hasItem(StockAdded("VOD.L")))
    }

    @Test
    fun doesnt_add_the_same_stock_twice() {
        watchlistProcessor.process(AddStock("VOD.L"))
        watchlistProcessor.process(AddStock("VOD.L"))

        watchlist.catchup()

        assertThat(store.count { (it as? StockAdded)?.stockId == "VOD.L" }, equalTo(1))
        assertThat(watchlist.count { it.stockId == "VOD.L" }, equalTo(1))
    }

    @Test
    fun add_user_watch() {
        watchlistProcessor.process(AddStock("VOD.L", "Bob"))

        watchlist.catchup()

        assertThat(watchlist, hasItem(Watch("VOD.L", "Bob")))
        assertThat(watchlist, not(hasItem(Watch("VOD.L"))))
    }

    @Test
    fun remove_user_watch() {
        watchlistProcessor.process(AddStock("VOD.L", "Bob"))
        watchlistProcessor.process(RemoveStock("VOD.L", "Bob"))

        watchlist.catchup()

        assertThat(watchlist, not(hasItem(Watch("VOD.L", "Bob"))))
        assertThat(watchlist, not(hasItem(Watch("VOD.L"))))
    }

    @Test
    fun concurrent_processors_maintain_consistency() {
        val processor1 = WatchlistProcessor(store, watchlist)
        val processor2 = WatchlistProcessor(store, watchlist)

        processor1.process(AddStock("VOD.L"))
        processor2.process(AddStock("VOD.L"))

        assertThat(store.count { (it as? StockAdded)?.stockId == "VOD.L" }, equalTo(1))
    }
}
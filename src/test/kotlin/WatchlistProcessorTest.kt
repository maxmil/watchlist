import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Test

class WatchlistProcessorTest {


    val store = mutableListOf<Event>()

    @Test
    fun add_system_wide_stock() {
        val watchlistProcessor = WatchlistProcessor(store)

        watchlistProcessor.process(AddStock("VOD.L"))

        assertThat(store, hasItem(StockAdded("VOD.L")))
    }

    @Test
    fun doesnt_add_the_same_stock_twice() {
        val watchlistProcessor = WatchlistProcessor(store)

        watchlistProcessor.process(AddStock("VOD.L"))
        watchlistProcessor.process(AddStock("VOD.L"))

        assertThat(store.count { (it as? StockAdded)?.ric == "VOD.L" }, equalTo(2))
        assertThat(watchlistProcessor.watchlist.count { it.ric == "VOD.L" }, equalTo(1))
    }

    @Test
    fun add_user_watch() {
        val watchlistProcessor = WatchlistProcessor(store)

        watchlistProcessor.process(AddStock("VOD.L", "Bob"))

        assertThat(watchlistProcessor.watchlist, hasItem(Watch("VOD.L", "Bob")))
        assertThat(watchlistProcessor.watchlist, not(hasItem(Watch("VOD.L"))))
    }

    @Test
    fun remove_user_watch() {
        val watchlistProcessor = WatchlistProcessor(store)

        watchlistProcessor.process(AddStock("VOD.L", "Bob"))
        watchlistProcessor.process(RemoveStock("VOD.L", "Bob"))

        assertThat(watchlistProcessor.watchlist, not(hasItem(Watch("VOD.L", "Bob"))))
        assertThat(watchlistProcessor.watchlist, not(hasItem(Watch("VOD.L"))))
    }
}
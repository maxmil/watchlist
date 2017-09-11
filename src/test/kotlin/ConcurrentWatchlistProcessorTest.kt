import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ConcurrentWatchlistProcessorTest {


    @Test
    fun concurrent_processors_cannot_add_same_watch_twice() {
            val (store, processors) = concurrentAddStock(threads = 2, retries = 2, stockIdForIndex = { "1" })

            assertThat(processors.failures(), equalTo(0))
            assertThat(store.size, equalTo(1))
    }

    @Test
    fun concurrent_processors_cannot_add_with_two_threads_and_one_retry() {
        val (_, processors) = concurrentAddStock(threads = 2, retries = 1)

        assertThat(processors.failures(), equalTo(1))
    }

    @Test
    fun concurrent_processors_add_with_two_threads_two_retries() {

        val (store, processors) = concurrentAddStock(threads = 2, retries = 2)

        assertThat(processors.failures(), equalTo(0))

        assertThat(store, hasItem(StockAdded("1")))
        assertThat(store, hasItem(StockAdded("2")))
    }

    private fun concurrentAddStock(threads: Int, retries: Int, stockIdForIndex: (Int) -> String = { i -> i.toString() }): Pair<DelegatingEventStore, List<Future<*>>> {
        val latch = CountDownLatch(threads)
        val store = DelegatingEventStore(BlockingEventStore(), latch)
        val watchlist = Watchlist(store)

        val service = Executors.newFixedThreadPool(threads)
        val processors = (1..threads).map {
            service.submit {
                WatchlistProcessor(store, watchlist, retries).process(AddStock(stockIdForIndex.invoke(it)))
            }
        }
        return Pair(store, processors)
    }

    class DelegatingEventStore(private val delegate: EventStore, val latch: CountDownLatch) : EventStore by delegate {
        override fun write(event: Event, position: Int): Boolean {
            latch.countDown()
            latch.await()
            return delegate.write(event, position)
        }
    }

    fun List<Future<*>>.failures() = count { future ->
        try {
            future.get()
            false
        } catch (e: ExecutionException) {
            true
        }
    }
}
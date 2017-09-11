class WatchlistProcessor(val store: EventStore, val watchlist: Watchlist, val retries:Int = 3) {

    fun process(command: Command) {
        for (i in 1..retries) {
            if (tryToProcess(command)) return
        }
        throw RuntimeException("Unable to process command $command")
    }

    private fun tryToProcess(command: Command): Boolean {
        val nextPosition = watchlist.catchup()
        return when (command) {
            is AddStock -> {
                if (!watchlist.contains(Watch(command.stockId, command.user))) {
                    store.write(StockAdded(command.stockId, command.user), nextPosition)
                } else {
                    true
                }
            }
            is RemoveStock -> {
                if (watchlist.contains(Watch(command.stockId, command.user))) {
                    store.write(StockRemoved(command.stockId, command.user), nextPosition)
                } else {
                    true
                }
            }
        }
    }
}

class Watchlist(private val store: EventStore,
                private val watches: MutableSet<Watch> = mutableSetOf()) : Set<Watch> by watches {

    private var position: Int = 0

    fun catchup(): Int {
        while (position < store.size) {
            apply(store[position])
            position++
        }
        return position
    }

    private fun apply(event: Event) {
        when (event) {
            is StockAdded -> watches.add(Watch(event.stockId, event.user))
            is StockRemoved -> watches.remove(Watch(event.stockId, event.user))
        }
    }
}

interface EventStore : List<Event> {
    fun write(event: Event, position: Int): Boolean
}

class BlockingEventStore(private val store: MutableList<Event> = mutableListOf()) : List<Event> by store, EventStore {

    override fun write(event: Event, position: Int): Boolean =
            synchronized(this) {
                if (store.size == position) store.add(event) else false
            }
}

sealed class Command
data class AddStock(val stockId: String, val user: String? = null) : Command()
data class RemoveStock(val stockId: String, val user: String? = null) : Command()

sealed class Event
data class StockAdded(val stockId: String, val user: String? = null) : Event()
data class StockRemoved(val stockId: String, val user: String? = null) : Event()

data class Watch(val stockId: String, val user: String? = null)
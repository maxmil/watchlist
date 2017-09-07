

class WatchlistProcessor(val store:MutableList<Event>,
                         val watchlist:MutableSet<Watch> = mutableSetOf()) {

    fun process(command: Command) {
        val event = when (command) {
            is AddStock -> StockAdded(command.ric, command.user)
            is RemoveStock -> StockRemoved(command.ric, command.user)
        }
        store.add(event)
        apply(event)
    }

    fun apply(event:Event) {
        when (event) {
            is StockAdded -> watchlist.add(Watch(event.ric, event.user))
            is StockRemoved -> watchlist.remove(Watch(event.ric, event.user))
        }
    }
}

sealed class Command
data class AddStock(val ric: String, val user:String? = null) : Command()
data class RemoveStock(val ric: String, val user:String? = null) : Command()

sealed class Event
data class StockAdded(val ric: String, val user:String? = null) : Event()
data class StockRemoved(val ric: String, val user:String? = null) : Event()

data class Watch(val ric: String, val user:String? = null) : Event()
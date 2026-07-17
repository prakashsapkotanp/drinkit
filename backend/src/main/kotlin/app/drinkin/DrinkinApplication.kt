package app.drinkin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
class DrinkinApplication {
    companion object {
        init {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            System.setProperty("user.timezone", "UTC")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<DrinkinApplication>(*args)
}

package app.drinkin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DrinkinApplication

fun main(args: Array<String>) {
    runApplication<DrinkinApplication>(*args)
}

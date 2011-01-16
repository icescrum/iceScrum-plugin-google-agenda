package icescrum.plugin.google.agenda

import org.icescrum.core.domain.Product

class GoogleAccount {

    String login
    String password

    static constraints = {
      login(nullable:false)
      password(nullable:false)
    }

    static belongsTo = [product:Product]
}
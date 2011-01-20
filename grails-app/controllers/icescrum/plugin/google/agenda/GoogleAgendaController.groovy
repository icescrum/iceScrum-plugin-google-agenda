package icescrum.plugin.google.agenda

import grails.plugins.springsecurity.Secured
import org.icescrum.web.support.MenuBarSupport
import org.icescrum.core.domain.Product

@Secured('scrumMaster()')
class GoogleAgendaController {
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.ui.googleAgenda',id , false, 3)
    static window =  [title:'is.ui.googleAgenda',help:'is.ui.googleAgenda.help',toolbar:false]

    def index = {
        GoogleAccount projectAccount = GoogleAccount.findByProduct(Product.get(params.product))
        if (projectAccount) {
            render template:'displayAccount',
                  plugin:'iceScrum-plugin-google-agenda',
                  model:[login:projectAccount.login]
        }
        else {
            render template:'setAccount',
                plugin:'iceScrum-plugin-google-agenda',
                model:[id:id]
        }
    }

    def saveAccount = {
        GoogleAccount projectAccount = new GoogleAccount(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        projectAccount.save()
        redirect (action:'index')
    }
}

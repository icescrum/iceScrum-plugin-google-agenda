package icescrum.plugin.google.agenda
import grails.plugins.springsecurity.Secured
import org.icescrum.web.support.MenuBarSupport

class GoogleAgendaController {
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.ui.googleAgenda',id , false, 9)
    static window =  [title:'is.ui.googleAgenda',help:'is.ui.googleAgenda.help',toolbar:false]
    def index = {
       render template:'setAccount',
              plugin:'iceScrum-plugin-google-agenda',
              model:[]
    }

    def saveAccount = {
       render template:'setAccount',
              plugin:'iceScrum-plugin-google-agenda'
    }
}

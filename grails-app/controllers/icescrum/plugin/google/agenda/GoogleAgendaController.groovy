package icescrum.plugin.google.agenda

class GoogleAgendaController {
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = [show:[visible:true,pos:0],title:'is.ui.googleAgenda']
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

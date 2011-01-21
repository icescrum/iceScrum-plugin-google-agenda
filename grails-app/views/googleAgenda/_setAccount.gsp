<form id="googleForm" name="googleForm" method="post" class='box-form box-form-small-legend box-content box-form-180' onsubmit="$('input[name=googleButton]').click();return false;">
    <is:fieldset title="is.googleAgenda.ui.googleAgendaAdmin">
        <is:fieldInformation nobordertop="false">
          <g:message code="is.ui.googleAgenda.ui.setAccountDescription"/>
        </is:fieldInformation>
        <is:fieldInput for="googleLogin" label="is.googleAgenda.ui.googleLogin">
          <is:input id="googleLogin" name="googleLogin" />
        </is:fieldInput>
        <is:fieldInput for="googlePassword" label="is.googleAgenda.ui.googlePassword">
          <is:password id="googlePassword" name="googlePassword" />
        </is:fieldInput>
        <is:buttonBar id="googleButtonBar">
          <is:button
                    type="submit"
                    url="[controller:id, action:'saveAccount', params:[product:params.product]]"
                    value="${message(code:'is.googleAgenda.ui.connectAccount')}" />
        </is:buttonBar>
    </is:fieldset>
</form>
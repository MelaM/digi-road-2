(function (root) {
  root.LinkPropertyForm = function(selectedLinkProperty) {
    var compactForm = false;

    var discontinuities = [
      [1, 'Tien loppu'],
      [2, 'Epäjatkuva'],
      [3, 'ELY:n raja'],
      [4, 'Lievä epäjatkuvuus'],
      [5, 'Jatkuva']
    ];

    var getDiscontinuityType = function(discontinuity){
      var DiscontinuityType = _.find(discontinuities, function(x){return x[0] === discontinuity;});
      return DiscontinuityType && DiscontinuityType[1];
    };

    var checkIfMultiSelection = function(mmlId){
      if(selectedLinkProperty.count() == 1){
        return mmlId;
      }
    };

    var dynamicField = function(labelText){
      var floatingTransfer = (!applicationModel.isReadOnly() && compactForm);
      var field;
      //If other fields get the same treatment they can be added here
      if(labelText === 'TIETYYPPI'){
        var roadTypes = "";
        _.each(selectedLinkProperty.get(), function(slp){
          var roadType = slp.roadType;
          if (roadTypes.length === 0) {
            roadTypes = roadType;
          } else if(roadTypes.search(roadType) === -1) {
            roadTypes = roadTypes + ", " + roadType;
          }
        });
        if(floatingTransfer){
          field = '<div class="form-group">' +
            '<label class="control-label-floating">' + labelText + '</label>' +
            '<p class="form-control-static-floating">' + roadTypes + '</p>' +
            '</div>' ;
        } else {
          field = '<div class="form-group">' +
            '<label class="control-label">' + labelText + '</label>' +
            '<p class="form-control-static">' + roadTypes + '</p>' +
            '</div>';
          }
      } else if(labelText === 'VALITUT LINKIT'){
        var sources = !_.isEmpty(selectedLinkProperty.getSources()) ? selectedLinkProperty.getSources() : selectedLinkProperty.get();
        field = formFields(sources);
      }
      return field;
    };

    var formFields = function (sources){
      var linkIds = "";
      var field;
      var id = 0;
      _.each(sources, function(slp){
        var divId = "VALITUTLINKIT" + id;
        var linkid = slp.linkId.toString();
        if (linkIds.length === 0) {
          field = '<div class="form-group" id=' +divId +'>' +
            '<label class="control-label-floating">' + 'LINK ID:' + '</label>' +
            '<p class="form-control-static-floating">' + linkid + '</p>' +
            '</div>' ;
          linkIds = linkid;
        } else if(linkIds.search(linkid) === -1){
          field = field + '<div class="form-group" id=' +divId +'>' +
            '<label class="control-label-floating">' + 'LINK ID:' + '</label>' +
            '<p class="form-control-static-floating">' + linkid + '</p>' +
            '</div>' ;
          linkIds = linkIds + ", " + linkid;
        }
        id = id + 1;
      });
      return field;
    };

    var additionalSource = function(linkId, marker) {
        return (!_.isUndefined(marker)) ? '' +
        '<div class = "form-group" id = "aditionalSource">' +
        '<div style="display:inline-flex;justify-content:center;align-items:center;">' +
        '<label class="control-label-floating"> LINK ID:</label>' +
        '<span class="form-control-static-floating" style="display:inline-flex;width:auto;margin-right:5px">' + linkId + '</span>' +
        '<span class="marker">' + marker + '</span>' +
        '<button class="add-source btn btn-new" id="aditionalSourceButton-' + linkId + '" value="' + linkId + '">Lisää kelluva tieosoite</button>' +
        '</div>' +
        '</div>' : '' +
        '<div class = "form-group" id = "aditionalSource">' +
        '<div style="display:inline-flex;justify-content:center;align-items:center;">' +
        '<label class="control-label-floating"> LINK ID:</label>' +
        '<span class="form-control-static-floating" style="display:inline-flex;width:auto;margin-right:5px">' + linkId + '</span>' +
        '</div>' +
        '</div>';
    };

    var adjacentsTemplate = '' +
      '<div class="target-link-selection" id="adjacentsData">' +
      '<div class="form-group" id="adjacents">' +
      '<% if(!_.isEmpty(adjacentLinks)){ %>' +
      '<br><br><label class="control-label-adjacents">VALITTAVISSA OLEVAT TIELINKIT, JOILTA PUUTTUU TIEOSOITE:</label>' +
      ' <% } %>' +
      '<% _.forEach(adjacentLinks, function(l) { %>' +
      '<div style="display:inline-flex;justify-content:center;align-items:center;">' +
      '<label class="control-label-floating"> LINK ID: </label>' +
      '<span class="form-control-static-floating" style="display:inline-flex;width:auto;margin-right:5px"><%= l.linkId %></span>' +
      '<span class="marker"><%= l.marker %></span>' +
      '<button class="select-adjacent btn btn-new" id="sourceButton-<%= l.linkId %>" value="<%= l.linkId %>">Valitse</button>' +
      '</div>' +
      '</span>' +
      '</label>' +
      ' <% }) %>' +
      '</div>' +
      '</div>';

    var afterCalculationTemplate ='' +
      '<div class="form-group" id="afterCalculationInfo">' +
      ' <br><br><p><span style="margin-top:6px; color:#ffffff; padding-top:6px; padding-bottom:6px; line-height:15px;">TARKISTA TEKEMÄSI MUUTOKSET KARTTANÄKYMÄSTÄ.</span></p>' +
      ' <p><span style="margin-top:6px; color:#ffffff; padding-top:6px; padding-bottom:6px; line-height:15px;">JOS TEKEMÄSI MUUTOKSET OVAT OK, PAINA TALLENA</span></p>' +
      ' <p><span style="margin-top:6px; color:#ffffff; padding-top:6px; padding-bottom:6px; line-height:15px;">JOS HALUAT KORJATA TEKEMÄSI MUUTOKSIA, PAINA PERUUTA</span></p>' +
      '</div>';

    var staticField = function(labelText, dataField) {
      var floatingTransfer = (!applicationModel.isReadOnly() && compactForm);
      var field;
      if(floatingTransfer){
        field = '<div class="form-group">' +
        '<label class="control-label-floating">' + labelText + '</label>' +
        '<p class="form-control-static-floating"><%- ' + dataField + ' %></p>' +
        '</div>';
      } else {
        field = '<div class="form-group">' +
        '<label class="control-label">' + labelText + '</label>' +
        '<p class="form-control-static"><%- ' + dataField + ' %></p>' +
        '</div>';
      }
      return field;
    };

    var title = function() {
      return '<span>Tieosoitteen ominaisuustiedot</span>';
    };

    var buttons =
      '<div class="link-properties form-controls">' +
      '<button class="calculate btn btn-move" disabled>Siirrä</button>' +
      '<button class="save btn btn-tallena" disabled>Tallenna</button>' +
      '<button class="cancel btn btn-perruta" disabled>Peruuta</button>' +
      '</div>';

    var notificationFloatingTransfer = function(displayNotification) {
      if(displayNotification)
        return '' +
          '<div class="form-group form-notification">' +
          '<p>Tien geometria on muuttunut. Korjaa tieosoitesegmentin sijainti valitsemalla ensimmäinen kohdelinkki, jolle haluat siirtää tieosoitesegmentin.</p>' +
          '</div>';
      else
        return '';
    };


    var template = function() {
      //VIITE-198
      //var endDateField = selectedLinkProperty.count() == 1 && typeof selectedLinkProperty.get()[0].endDate !== 'undefined' ?
      //staticField('LAKKAUTUS', 'endDate') : '';
      var roadTypes = selectedLinkProperty.count() == 1 ? staticField('TIETYYPPI', 'roadType') : dynamicField('TIETYYPPI');
      return _.template('' +
        '<header>' +
        title() + buttons +
        '</header>' +
        '<div class="wrapper read-only">' +
        '<div class="form form-horizontal form-dark">' +
        '<div class="form-group">' +
        '<p class="form-control-static asset-log-info">Muokattu viimeksi: <%- modifiedBy %> <%- modifiedAt %></p>' +
        '</div>' +
        '<div class="form-group">' +
        '<p class="form-control-static asset-log-info">Linkkien lukumäärä: ' + selectedLinkProperty.count() + '</p>' +
        '</div>' +
        staticField('TIENUMERO', 'roadNumber') +
        staticField('TIEOSANUMERO', 'roadPartNumber') +
        staticField('AJORATA', 'trackCode') +
        staticField('ALKUETÄISYYS', 'startAddressM') +
        staticField('LOPPUETÄISUUS', 'endAddressM') +
        staticField('ELY', 'elyCode') +
        roadTypes +
        staticField('JATKUVUUS', 'discontinuity') +
        '</div>' +
        '<footer>' + buttons + '</footer>');
    };

    var templateFloating = function() {
      var roadTypes = selectedLinkProperty.count() == 1 ? staticField('TIETYYPPI', 'roadType') : dynamicField('TIETYYPPI');
      return _.template('' +
        '<header>' +
        title() + buttons +
        '</header>' +
        '<div class="wrapper read-only-floating">' +
        '<div class="form form-horizontal form-dark">' +
        '<div class="form-group">' +
        '<p class="form-control-static asset-log-info">Muokattu viimeksi: <%- modifiedBy %> <%- modifiedAt %></p>' +
        '</div>' +
        '<div class="form-group">' +
        '<p class="form-control-static asset-log-info">Linkkien lukumäärä: ' + selectedLinkProperty.count() + '</p>' +
        '</div>' +
        staticField('TIENUMERO', 'roadNumber') +
        staticField('TIEOSANUMERO', 'roadPartNumber') +
        staticField('AJORATA', 'trackCode') +
        roadTypes +
        notificationFloatingTransfer(true)   +
        '</div>' +
        '</div>' +
        '<footer>' + buttons + '</footer>');
    };

    var templateFloatingEditMode = function() {
      var roadTypes = selectedLinkProperty.count() == 1 ? staticField('TIETYYPPI', 'roadType') : dynamicField('TIETYYPPI');
      var linkIds = dynamicField('VALITUT LINKIT');
      return _.template('<div style="display: none" id="floatingEditModeForm">' +
        '<header>' +
        title() +
        '</header>' +
        '<div class="wrapper edit-mode-floating">' +
        '<div class="form form-horizontal form-dark">' +
        '<div class="form-group">' +
        '<p class="form-control-static asset-log-info">Muokattu viimeksi: <%- modifiedBy %> <%- modifiedAt %></p>' +
        '</div>' +
        '<div class="form-group">' +
        '<p class="form-control-static asset-log-info">Linkkien lukumäärä: ' + selectedLinkProperty.count() + '</p>' +
        '</div>' +
        staticField('TIENUMERO', 'roadNumber') +
        staticField('TIEOSANUMERO', 'roadPartNumber') +
        staticField('AJORATA', 'trackCode') +
        roadTypes +
        notificationFloatingTransfer(true) +
        staticField('VALITUT LINKIT:', '') +
        linkIds  +
        '</div>' +
        '</div>' +
        '<footer>' + buttons + '</footer> </div>');
    };

    var addressNumberString = function(minAddressNumber, maxAddressNumber) {
      if(!minAddressNumber && !maxAddressNumber) {
        return '';
      } else {
        var min = minAddressNumber || '';
        var max = maxAddressNumber || '';
        return min + '-' + max;
      }
    };

    var bindEvents = function() {

      var rootElement = $('#feature-attributes');
      var toggleMode = function(readOnly) {
        rootElement.find('.editable .form-control-static').toggle(readOnly);
        rootElement.find('select').toggle(!readOnly);
        rootElement.find('.form-controls').toggle(!readOnly);
        rootElement.find('.btn-move').toggle(false);
        if(compactForm && !_.isEmpty(selectedLinkProperty.get())){

          if(!applicationModel.isReadOnly()){
            rootElement.html(templateFloatingEditMode(selectedLinkProperty.get()[0])(selectedLinkProperty.get()[0]));
            $('#floatingEditModeForm').show();
          } else {
            rootElement.html(templateFloating(selectedLinkProperty.get()[0])(selectedLinkProperty.get()[0]));
          }
          rootElement.find('.form-controls').toggle(!readOnly && compactForm);
          rootElement.find('.btn-move').toggle(!readOnly && compactForm);
        }
      };
      eventbus.on('linkProperties:selected linkProperties:cancelled', function(linkProperties) {
        if(!_.isEmpty(selectedLinkProperty.get())){
          compactForm = !_.isEmpty(selectedLinkProperty.get()) && (selectedLinkProperty.get()[0].roadLinkType === -1 || selectedLinkProperty.getFeaturesToKeep().length >= 1);
          if(compactForm && !applicationModel.isReadOnly() && selectedLinkProperty.getFeaturesToKeep().length > 1)
            selectedLinkProperty.getLinkAdjacents(selectedLinkProperty.get()[0]);
          linkProperties.modifiedBy = linkProperties.modifiedBy || '-';
          linkProperties.modifiedAt = linkProperties.modifiedAt || '';
          linkProperties.roadNameFi = linkProperties.roadNameFi || '';
          linkProperties.roadNameSe = linkProperties.roadNameSe || '';
          linkProperties.roadNameSm = linkProperties.roadNameSm || '';
          linkProperties.mmlId = checkIfMultiSelection(linkProperties.mmlId) || '';
          linkProperties.roadAddress = linkProperties.roadAddress || '';
          linkProperties.segmentId = linkProperties.segmentId || '';
          linkProperties.roadNumber = linkProperties.roadNumber || '';
          if (linkProperties.roadNumber > 0) {
            linkProperties.roadPartNumber = linkProperties.roadPartNumber || '';
            linkProperties.startAddressM = linkProperties.startAddressM || '0';
            linkProperties.trackCode = isNaN(parseFloat(linkProperties.trackCode)) ? '' : parseFloat(linkProperties.trackCode);
          } else {
            linkProperties.roadPartNumber = '';
            linkProperties.trackCode = '';
            linkProperties.startAddressM = '';
          }
          linkProperties.elyCode = isNaN(parseFloat(linkProperties.elyCode)) ? '' : linkProperties.elyCode;
          linkProperties.endAddressM = linkProperties.endAddressM || '';
          linkProperties.discontinuity = getDiscontinuityType(linkProperties.discontinuity) || '';
          linkProperties.roadType = linkProperties.roadType || '';
          linkProperties.roadLinkType = linkProperties.roadLinkType || '';

          if (compactForm){
            if(!applicationModel.isReadOnly()){
              rootElement.html(templateFloatingEditMode(linkProperties)(linkProperties));
          } else {
              rootElement.html(templateFloating(linkProperties)(linkProperties));
            }
          } else {
            rootElement.html(template(linkProperties)(linkProperties));
          }
          toggleMode(applicationModel.isReadOnly());
        }
      });
      eventbus.on('adjacents:added', function(sources, targets) {
        processAdjacents(sources,targets);
        applicationModel.removeSpinner();

      });

      eventbus.on('adjacents:aditionalSourceFound', function(sources, targets, additionalSourceLinkId) {
        $('#aditionalSource').remove();
        $('#adjacentsData').remove();
        processAdjacents(sources, targets, additionalSourceLinkId);
        applicationModel.removeSpinner();
      });

      var processAdjacents = function (sources, targets, additionalSourceLinkId) {
        var adjacents = _.reject(targets, function(t){
          return t.roadLinkType == -1;
        });

        //singleLinkSelection case
        var floatingAdjacents = [];
          if(selectedLinkProperty.count() === 1)
            floatingAdjacents = _.filter(targets, function(t){
            return t.roadLinkType == -1;
          });

        var fullTemplate = applicationModel.getCurrentAction() === applicationModel.actionCalculated ? afterCalculationTemplate : !_.isEmpty(floatingAdjacents) ? _.map(floatingAdjacents, function(fa){
          return additionalSource(fa.linkId, fa.marker);
        })[0] + adjacentsTemplate : adjacentsTemplate;

        if(!_.isUndefined(additionalSourceLinkId)){
          return $(".form-group[id^='VALITUTLINKIT']:last").append('<div style="display:inline-flex;justify-content:center;align-items:center;">' +
            '<label class="control-label-floating"> LINK ID:</label>' +
            '<span class="form-control-static-floating" style="display:inline-flex;width:auto;margin-right:5px">' + additionalSourceLinkId + '</span>' +
            '</div>');
        }

        if($(".form-group[id^='VALITUTLINKIT']:last")[0].childNodes.length <=2){
            $(".form-group[id^='VALITUTLINKIT']:last").append($(_.template(fullTemplate)(_.merge({}, {"adjacentLinks": adjacents}))));
            $('#floatingEditModeForm').show();
            if(applicationModel.getCurrentAction() === applicationModel.actionCalculated)
              eventbus.trigger("adjacents:roadTransfer");
            $('[id*="sourceButton"]').click({"sources": sources, "adjacents": adjacents},function(event) {
              eventbus.trigger("adjacents:nextSelected", event.data.sources, event.data.adjacents, event.currentTarget.value);
              rootElement.find('.link-properties button.calculate').attr('disabled', false);
              rootElement.find('.link-properties button.cancel').attr('disabled', false);
              applicationModel.setActiveButtons(true);
            });
            $('[id*="aditionalSourceButton"]').click(sources,function(event) {
              applicationModel.addSpinner();
              eventbus.trigger("adjacents:additionalSourceSelected", sources, event.currentTarget.value);
              rootElement.find('.link-properties button.cancel').attr('disabled', false);
              applicationModel.setActiveButtons(true);
            });
        }

      };
      
      eventbus.on('linkProperties:changed', function() {
        rootElement.find('.link-properties button').attr('disabled', false);
      });
      eventbus.on('linkProperties:unselected', function() {
        rootElement.empty();
      });
      eventbus.on('application:readOnly', toggleMode);
      rootElement.on('click', '.link-properties button.save', function() {
        if(applicationModel.getCurrentAction() === applicationModel.actionCalculated)
        {
          selectedLinkProperty.saveTransfer();
        } else {
          selectedLinkProperty.save();
        }
      });
      rootElement.on('click', '.link-properties button.cancel', function() {
        var action;
        if(applicationModel.isActiveButtons())
          action = applicationModel.actionCalculating;
        applicationModel.setCurrentAction(action);
        selectedLinkProperty.cancel(action);
        applicationModel.setActiveButtons(false);
      });
      rootElement.on('click', '.link-properties button.calculate', function() {
        applicationModel.addSpinner();
        selectedLinkProperty.transferringCalculation();
        applicationModel.setActiveButtons(true);
        
      });
      eventbus.on('adjacents:roadTransfer', function(result, sourceIds, targets) {
        $('#aditionalSource').remove();
        $('#adjacentsData').remove();
        rootElement.find('.link-properties button.save').attr('disabled', false);
        rootElement.find('.link-properties button.cancel').attr('disabled', false);
        rootElement.find('.link-properties button.calculate').attr('disabled', true);
        $('[id^=VALITUTLINKIT]').remove();

        var fields = formFields(_.map(targets, function(sId){
          return {'linkId' : sId};
        })) + '' + afterCalculationTemplate;

        $('.form-group:last').after(fields);

        applicationModel.removeSpinner();
      });

      eventbus.on('adjacents:startedFloatingTransfer', function() {
        rootElement.find('.link-properties button.cancel').attr('disabled', false);
        applicationModel.setActiveButtons(true);
      });
    };
    bindEvents();
  };
})(this);

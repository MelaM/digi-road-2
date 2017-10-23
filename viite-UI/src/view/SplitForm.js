(function (root) {
  root.SplitForm = function(projectCollection, selectedProjectLinkProperty, projectLinkLayer, projectChangeTable) {
    var LinkStatus = LinkValues.LinkStatus;
    var LinkGeomSource = LinkValues.LinkGeomSource;
    var CalibrationCode = LinkValues.CalibrationCode;
    var currentProject = false;
    var selectedProjectLink = false;
    var markers = ['A', 'B'];
    var backend=new Backend();
    var staticField = function(labelText, dataField) {
      var field;
      field = '<div class="split-form-group">' +
          '<p class="form-control-static asset-log-info">' + labelText + ' : ' + dataField + '</p>' +
          '</div>';
      return field;
    };
    var actionSelectedField = function() {
      var field;
      field = '<div class="split-form-group action-selected-field" hidden = "true">' +
          '<div class="asset-log-info">' + 'Tarkista tekemäsi muutokset.' + '<br>' + 'Jos muutokset ok, tallenna.' + '</div>' +
          '</div>';
      return field;
    };
    var endDistanceOriginalValue = '--';
    var options =['Valitse'];

    var title = function() {
      return '<span class ="edit-mode-title">Uusi tieosoiteprojekti</span>';
    };

    var titleWithProjectName = function(projectName) {
      return '<span class ="edit-mode-title">'+projectName+'<button id="editProject_'+ currentProject.id +'" ' +
          'class="btn-edit-project" style="visibility:hidden;" value="' + currentProject.id + '"></button></span>' +
          '<span id="closeProjectSpan" class="rightSideSpan" style="visibility:hidden;">Poistu projektista</span>';
    };

    var clearInformationContent = function() {
      $('#information-content').empty();
    };

    var sendRoadAddressChangeButton = function() {

      return '<div class="split-form form-controls">' +
          '<button class="show-changes btn btn-block btn-show-changes">Avaa projektin yhteenvetotaulukko</button>' +
          '<button id ="send-button" class="send btn btn-block btn-send">Tee tieosoitteenmuutosilmoitus</button></div>';
    };

    var showProjectChangeButton = function() {
      return '<div class="split-form form-controls">' +
          '<button class="show-changes btn btn-block btn-show-changes">Avaa projektin yhteenvetotaulukko</button>' +
          '<button disabled id ="send-button" class="send btn btn-block btn-send">Tee tieosoitteenmuutosilmoitus</button></div>';
    };

    var actionButtons = function() {
      var html = '<div class="split-form form-controls" id="actionButtons">' +
          '<button class="update btn btn-save"' + (projectCollection.isDirty() ? '' : 'disabled') + ' style="width:auto;">Tallenna</button>' +
          '<button class="cancelLink btn btn-cancel">Peruuta</button>' +
          '</div>';
      return html;
    };

    var selectedData = function (selected) {
      var span = [];
      if (selected[0]) {
        var link = selected[0];
        var startM = Math.min.apply(Math, _.map(selected, function(l) { return l.startAddressM; }));
        var endM = Math.max.apply(Math, _.map(selected, function(l) { return l.endAddressM; }));
        var div = '<div class="project-edit-selections" style="display:inline-block;padding-left:8px;">' +
            '<div class="project-edit">' +
            ' TIE ' + '<span class="project-edit">' + link.roadNumber + '</span>' +
            ' OSA ' + '<span class="project-edit">' + link.roadPartNumber + '</span>' +
            ' AJR ' + '<span class="project-edit">' + link.trackCode + '</span>' +
            ' M:  ' + '<span class="project-edit">' + startM + ' - ' + endM + '</span>' +
            (selected.length > 1 ? ' (' + selected.length + ' linkkiä)' : '')+
            '</div>' +
            '</div>';
        span.push(div);
      }
      return span;
    };

    var selectedSplitData = function (selected) {
      var span = [];
      _.each(selected, function(sel){
        if (sel) {
          var link = sel;
          var startM = ((applicationModel.getSelectedTool() === 'Cut' && selected.length == 2) ? Math.round(link.startMValue) : Math.min.apply(Math, _.map(selected, function(l) { return l.startAddressM; })));
          var endM = ((applicationModel.getSelectedTool() === 'Cut' && selected.length == 2) ? Math.round(link.endMValue) : Math.max.apply(Math, _.map(selected, function(l) { return l.endAddressM; })));
          var div = '<div class="project-edit-selections" style="display:inline-block;padding-left:8px;">' +
              '<div class="project-edit">' +
              ' TIE ' + '<span class="project-edit">' + link.roadNumber + '</span>' +
              ' OSA ' + '<span class="project-edit">' + link.roadPartNumber + '</span>' +
              ' AJR ' + '<span class="project-edit">' + link.trackCode + '</span>' +
              ' M:  ' + '<span class="project-edit">' + startM + ' - ' + endM + '</span>' +
              '</div>' +
              '</div>';
          span.push(div);
        }
      });
      return span;
    };

    var defineOptionModifiers = function(option, selection) {
      var roadIsUnknownOrOther = projectCollection.roadIsUnknown(selection[0]) || projectCollection.roadIsOther(selection[0]) || selection[0].roadLinkSource === LinkGeomSource.SuravageLinkInterface.value;
      var roadIsSuravage = selection[0].roadLinkSource === LinkGeomSource.SuravageLinkInterface.value;
      var isSplitMode = selection.length == 2 && selection[0].linkId === selection[1].linkId && applicationModel.getSelectedTool() == "Cut";
      var toEdit = !isSplitMode && selection[0].id === 0;
      var linkStatus = selection[0].status;
      var modifiers = '';

      switch(option) {
        case LinkStatus.Unchanged.description: {
          if ((roadIsUnknownOrOther || roadIsSuravage) && !isSplitMode) {
            modifiers = 'disabled hidden';
          } else {
            modifiers = '';
          }
          break;
        }
        case LinkStatus.Transfer.description: {
          if ((roadIsUnknownOrOther || roadIsSuravage) && !isSplitMode) {
            modifiers = 'disabled hidden';
          } else if(toEdit){
            modifiers = 'disabled';
          }
          break;
        }
        case LinkStatus.New.description: {
          var enableStatusNew = (selection[0].status !== LinkStatus.NotHandled.value &&
              selection[0].status !== LinkStatus.Terminated.value) || roadIsSuravage;
          if(!roadIsUnknownOrOther) {
            if(!enableStatusNew)
              modifiers = 'disabled';
          }
          break;
        }
        case LinkStatus.Terminated.description: {
          if(roadIsUnknownOrOther || roadIsSuravage){
            modifiers = 'disabled hidden';
          } else {
            var status = _.uniq(_.map(selection, function(l) { return l.status; }));
            if (status.length == 1)
              status = status[0];
            else
              status = 0;
            if (status === LinkStatus.Terminated.value){
              modifiers = 'selected';
            } else if(selection[0].roadLinkSource === LinkGeomSource.SuravageLinkInterface.value) {
              modifiers = 'disabled';
            }
          }
          break;
        }
        case LinkStatus.Numbering.description: {
          if(roadIsUnknownOrOther || roadIsSuravage){
            modifiers = 'disabled hidden';
          } else if(toEdit) {
            modifiers = 'disabled';
          } else if(selection[0].status === LinkStatus.Terminated.value) {
            modifiers = 'hidden';
          }
          break;
        }
        case LinkStatus.Revert.description: {
          if(roadIsUnknownOrOther){
            modifiers = 'disabled hidden';
          } else if(toEdit) {
            modifiers = 'disabled';
          }
          break;
        }
        default: {
          modifiers = 'selected disabled hidden';
        }
      }
      return modifiers;
    };

    var selectedProjectLinkTemplate = function(project, optionTags, selected) {
      var selection = ((applicationModel.getSelectedTool() == 'Cut' && selected[0].roadLinkSource == LinkGeomSource.SuravageLinkInterface.value) ? selectedSplitData(selected) : selectedData(selected));
      return _.template('' +
          '<header>' +
          titleWithProjectName(project.name) +
          '</header>' +
          '<div class="wrapper read-only">'+
          '<div class="form form-horizontal form-dark">'+
          '<div class="edit-control-group choice-group">'+
          staticField('Lisätty järjestelmään', project.createdBy + ' ' + project.startDate)+
          staticField('Muokattu viimeksi', project.modifiedBy + ' ' + project.dateModified)+
          '<div class="split-form-group editable form-editable-roadAddressProject"> '+
              selectionFormCutted(selection, selected) +
          ((selected.size == 2 && selected[0].linkId === selected[1].linkId) ? changeDirection(): '') +
          actionSelectedField()+
          '</div>'+
          '</div>' +
          '</div>'+
          '</div>'+
          '<footer>' + actionButtons() + '</footer>');
    };

    var selectionFormCutted = function(selection, selected){
      return '<form id="roadAddressProjectFormCut" class="input-unit-combination split-form-group form-horizontal roadAddressProject">'+
          '<label>Toimenpiteet,' + selection[0]  + '</label>' +
          '<span class="marker">'+markers[0]+'</span>'+
          dropdownOption(0, selected) +
          '<hr class="horizontal-line"/>' +
          '<label>Toimenpiteet,' + selection[1]  + '</label>' +
          '<span class="marker">'+markers[1]+'</span>' +
          dropdownOption(1, selected)+
          newRoadAddressInfo(selected) +
          '</form>';
    };

    var dropdownOption = function(index, selected){
      return '<div class="input-unit-combination">' +
          '<select class="split-form-control" id="dropdown_'+index+'" size="1">'+
          '<option id="drop_'+ index +'_' + '" '+ defineOptionModifiers('', selected) +'>Valitse</option>'+
          '<option id="drop_'+ index +'_' + LinkStatus.Unchanged.description + '" value='+ LinkStatus.Unchanged.description+' ' + defineOptionModifiers(LinkStatus.Unchanged.description, selected) + '>Ennallaan</option>'+
          '<option id="drop_'+ index +'_' + LinkStatus.Transfer.description + '" value='+ LinkStatus.Transfer.description + ' ' + defineOptionModifiers(LinkStatus.Transfer.description, selected) + '>Siirto</option>'+
          '<option id="drop_'+ index +'_' + LinkStatus.New.description + '" value='+ LinkStatus.New.description + ' ' + defineOptionModifiers(LinkStatus.New.description, selected) +'>Uusi</option>'+
          '<option id="drop_'+ index +'_' + LinkStatus.Terminated.description + '" value='+ LinkStatus.Terminated.description + ' ' + defineOptionModifiers(LinkStatus.Terminated.description, selected) + '>Lakkautus</option>'+
          '<option id="drop_'+ index +'_' + LinkStatus.Numbering.description + '" value='+ LinkStatus.Numbering.description + ' ' + defineOptionModifiers(LinkStatus.Numbering.description, selected) + '>Numerointi</option>'+
          '<option id="drop_'+ index +'_' + LinkStatus.Revert.description + '" value='+ LinkStatus.Revert.description + ' ' + defineOptionModifiers(LinkStatus.Revert.description, selected) + '>Palautus aihioksi tai tieosoitteettomaksi</option>' +
          '</select>'+
          '</div>';
    };

    var newRoadAddressInfo = function(selected){
      return '<div class="split-form-group new-road-address" hidden>' +
          '<div><label></label></div><div><label style = "margin-top: 50px">TIEOSOITTEEN TIEDOT</label></div>' +
          addSmallLabel('TIE') + addSmallLabel('OSA') + addSmallLabel('AJR')+ addSmallLabel('ELY')  + addSmallLabel('JATKUU')+
          '</div>' +
          '<div class="split-form-group new-road-address" id="new-address-input1" hidden>'+
          addSmallInputNumber('tie',(selectedProjectLink[0].roadNumber !== 0 ? selectedProjectLink[0].roadNumber : '')) +
          addSmallInputNumber('osa',(selectedProjectLink[0].roadPartNumber !== 0 ? selectedProjectLink[0].roadPartNumber : '')) +
          addSmallInputNumber('ajr',(selectedProjectLink[0].trackCode !== 99 ? selectedProjectLink[0].trackCode : '')) +
          addSmallInputNumberDisabled('ely', selectedProjectLink[0].elyCode) +
          addSelect() +
          addSmallLabel('TIETYYPPI') +
          roadTypeDropdown() +
          ((selected.length == 2 && selected[0].linkId === selected[1].linkId) ? '' : distanceValue()) +
          '</div>';
    };

    var replaceAddressInfo = function() {
      if (selectedProjectLink[0].roadNumber === 0 && selectedProjectLink[0].roadPartNumber === 0 && selectedProjectLink[0].trackCode === 99 )
      {
        backend.getNonOverridenVVHValuesForLink(selectedProjectLink[0].linkId, function (response) {
          if (response.success) {
            $('#tie').val(response.roadNumber);
            $('#osa').val(response.roadPartNumber);
          }
        });
      }
    };

    var roadTypeDropdown = function() {
      return '<select class="split-form-control" id="roadTypeDropDown" size = "1" style="width: auto !important; display: inline">' +
          '<option value = "1">1 Yleinen tie</option>'+
          '<option value = "2">2 Lauttaväylä yleisellä tiellä</option>'+
          '<option value = "3">3 Kunnan katuosuus</option>'+
          '<option value = "4">4 Yleisen tien työmaa</option>'+
          '<option value = "5">5 Yksityistie</option>'+
          '<option value = "9">9 Omistaja selvittämättä</option>' +
          '<option value = "99">99 Ei määritelty</option>' +
          '</select>';
    };

    var distanceValue = function() {
      return '<div id="distanceValue" hidden>' +
          '<div class="split-form-group" style="margin-top: 15px">' +
          '<img src="images/calibration-point.svg" style="margin-right: 5px" class="calibration-point"/>' +
          '<label class="control-label-small" style="display: inline">ETÄISYYSLUKEMA VALINNAN</label>' +
          '</div>' +
          '<div class="split-form-group">' +
          '<label class="control-label-small" style="float: left; margin-top: 10px">ALLUSSA</label>' +
          addSmallInputNumber('beginDistance', '--') +
          '<label class="control-label-small" style="float: left;margin-top: 10px">LOPUSSA</label>' +
          addSmallInputNumber('endDistance', '--') +
          '<span id="manualCPWarning" class="manualCPWarningSpan">!</span>' +
          '</div></div>';
    };

    var addSelect = function(){
      return '<select class="form-select-control" id="discontinuityDropdown" size="1">'+
          '<option value = "5" selected disabled hidden>5 Jatkuva</option>'+
          '<option value="1" >1 Tien loppu</option>'+
          '<option value="2" >2 Epäjatkuva</option>'+
          '<option value="3" >3 ELY:n raja</option>'+
          '<option value="4" >4 Lievä epäjatkuvuus</option>'+
          '<option value="5" >5 Jatkuva</option>'+
          '</select>';
    };

    var changeDirection = function () {
      return '<div hidden class="split-form-group changeDirectionDiv" style="margin-top:15px">' +
          '<button class="split-form-group changeDirection btn btn-primary">Käännä kasvusuunta</button>' +
          '</div>';
    };

    var addSmallLabel = function(label){
      return '<label class="control-label-small">'+label+'</label>';
    };

    var addSmallInputNumber = function(id, value){
      //Validate only number characters on "onkeypress" including TAB and backspace
      return '<input type="text" onkeypress="return (event.charCode >= 48 && event.charCode <= 57) || (event.keyCode == 8 || event.keyCode == 9)' +
          '"class="split-form-control small-input roadAddressProject" id="'+id+'" value="'+(_.isUndefined(value)? '' : value )+'" onclick=""/>';
    };

    var addSmallInputNumberDisabled = function(id, value){
      return '<input type="text" class="split-form-control small-input roadAddressProject" id="'+id+'" value="'+(_.isUndefined(value)? '' : value )+'" readonly="readonly"/>';
    };

    var emptyTemplate = function(project) {
      return _.template('' +
          '<header style ="display:-webkit-inline-box;">' +
          titleWithProjectName(project.name) +
          '</header>' +
          '<footer>'+showProjectChangeButton()+'</footer>');
    };

    var isProjectPublishable = function(){
      return projectCollection.getPublishableStatus();
    };

    var checkInputs = function () {
      var rootElement = $('#feature-attributes');
      var inputs = rootElement.find('input');
      var filled = true;
      for (var i = 0; i < inputs.length; i++) {
        if (inputs[i].type === 'text' && !inputs[i].value) {
          filled = false;
        }
      }
      if (filled) {
        rootElement.find('.split-form button.update').prop("disabled", false);
      } else {
        rootElement.find('.split-form button.update').prop("disabled", true);
      }
    };

    var toggleAditionalControls = function(){
      $('[id^=editProject]').css('visibility', 'visible');
      $('#closeProjectSpan').css('visibility', 'visible');
    };

    var changeDropDownValue = function (statusCode) {
      var rootElement = $('#feature-attributes');
      if (statusCode === LinkStatus.Unchanged.value) {
        $("#dropDown_0" ).val(LinkStatus.Unchanged.description).change();
      }
      else if(statusCode === LinkStatus.New.value){
        $("#dropDown_0" ).val(LinkStatus.New.description).change();
        projectCollection.setTmpDirty(projectCollection.getTmpDirty().concat(selectedProjectLink));
        rootElement.find('.new-road-address').prop("hidden", false);
        if(selectedProjectLink[0].id !== 0)
          rootElement.find('.changeDirectionDiv').prop("hidden", false);
      }
      else if (statusCode == LinkStatus.Transfer.value) {
        $("select option[value*="+LinkStatus.New.action+"]").prop('disabled',true);
        $('#dropDown').val(LinkStatus.Transfer.action).change();
      }
      $('#discontinuityDropdown').val(selectedProjectLink[selectedProjectLink.length - 1].discontinuity);
      $('#roadTypeDropDown').val(selectedProjectLink[0].roadTypeId);
    };

    var fillDistanceValues = function (selectedLinks) {
      if (selectedLinks.length === 1 && selectedLinks[0].calibrationCode === CalibrationCode.AtBoth.value) {
        $('#beginDistance').val(selectedLinks[0].startAddressM);
        $('#endDistance').val(selectedLinks[0].endAddressM);
      } else {
        var orderedByStartM = _.sortBy(selectedLinks, function (l) {
          return l.startAddressM;
        });
        if (orderedByStartM[0].calibrationCode === CalibrationCode.AtBeginning.value) {
          $('#beginDistance').val(orderedByStartM[0].startAddressM);
        }
        if (orderedByStartM[orderedByStartM.length - 1].calibrationCode === CalibrationCode.AtEnd.value) {
          $('#endDistance').val(orderedByStartM[orderedByStartM.length - 1].endAddressM);
          endDistanceOriginalValue = orderedByStartM[orderedByStartM.length - 1].endAddressM;
        }
      }
    };

    var bindEvents = function() {

      var rootElement = $('#feature-attributes');
      var toggleMode = function(readOnly) {
        rootElement.find('.wrapper read-only').toggle();
      };

      eventbus.on('projectLink:splited', function(selected) {
        selectedProjectLink = selected;
        currentProject = projectCollection.getCurrentProject();
        clearInformationContent();
        rootElement.html(selectedProjectLinkTemplate(currentProject.project, options, selectedProjectLink));
        replaceAddressInfo();
        checkInputs();
        toggleAditionalControls();
        changeDropDownValue(selectedProjectLink[0].status);
      });

      eventbus.on('roadAddress:projectFailed', function() {
        applicationModel.removeSpinner();
      });

      eventbus.on('roadAddress:projectLinksUpdateFailed',function(errorCode){
        applicationModel.removeSpinner();
        if (errorCode == 400){
          return new ModalConfirm("Päivitys epäonnistui puutteelisten tietojen takia. Ota yhteyttä järjestelmätukeen.");
        } else if (errorCode == 401){
          return new ModalConfirm("Sinulla ei ole käyttöoikeutta muutoksen tekemiseen.");
        } else if (errorCode == 412){
          return new ModalConfirm("Täyttämättömien vaatimusten takia siirtoa ei saatu tehtyä. Ota yhteyttä järjestelmätukeen.");
        } else if (errorCode == 500){
          return new ModalConfirm("Siirto ei onnistunut taustajärjestelmässä tapahtuneen virheen takia, ota yhteyttä järjestelmätukeen.");
        } else {
          return new ModalConfirm("Siirto ei onnistunut taustajärjestelmässä tapahtuneen tuntemattoman virheen takia, ota yhteyttä järjestelmätukeen.");
        }
      });

      eventbus.on('roadAddress:projectLinksUpdated',function(data){
        eventbus.trigger('projectChangeTable:refresh');
        projectCollection.setTmpDirty([]);
        if (typeof data !== 'undefined' && typeof data.publishable !== 'undefined' && data.publishable) {
          eventbus.trigger('roadAddressProject:projectLinkSaved', data.id, data.publishable);
        }
        else {
          eventbus.trigger('roadAddressProject:projectLinkSaved', data.id, data.publishable);
        }
      });

      eventbus.on('roadAddress:projectSentSuccess', function() {
        new ModalConfirm("Muutosilmoitus lähetetty Tierekisteriin.");
        //TODO: make more generic layer change/refresh
        applicationModel.selectLayer('linkProperty');

        rootElement.empty();
        clearInformationContent();

        selectedProjectLinkProperty.close();
        projectCollection.clearRoadAddressProjects();
        projectCollection.reset();
        applicationModel.setOpenProject(false);

        eventbus.trigger('roadAddressProject:deselectFeaturesSelected');
        eventbus.trigger('roadLinks:refreshView');
      });

      eventbus.on('roadAddress:projectSentFailed', function(error) {
        new ModalConfirm(error);
      });

      eventbus.on('roadAddress:projectLinksCreateSuccess', function () {
        eventbus.trigger('projectChangeTable:refresh');
        projectCollection.setTmpDirty([]);
        rootElement.find('.changeDirectionDiv').prop("hidden", false);
      });

      eventbus.on('roadAddress:changeDirectionFailed', function(error) {
        new ModalConfirm(error);
      });

      rootElement.on('click','.changeDirection', function () {
          if(applicationModel.getSelectedTool() == "Cut") {
              projectCollection.changeNewProjectLinkDirection(projectCollection.getCurrentProject().project.id, selectedProjectLinkProperty.get());
          }
      });

      eventbus.on('roadAddress:projectLinksSaveFailed', function (result) {
          if(applicationModel.getSelectedTool() == "Cut") {
              new ModalConfirm(result.toString());
          }
      });

      eventbus.on('roadAddressProject:discardChanges',function(){
        if(applicationModel.getSelectedTool() == "Cut") {
          cancelChanges();
        }
      });

      var saveChanges = function(){
        currentProject = projectCollection.getCurrentProject();
        //TODO revert dirtyness if others than ACTION_TERMINATE is choosen, because now after Lakkautus, the link(s) stay always in black color
        var statusDropdown_0 =$('#dropdown_0').val();
        var statusDropdown_1 = $('#dropdown_1').val();
        switch (statusDropdown_0){
          case LinkStatus.Unchanged.description : {
            if(!_.isUndefined(statusDropdown_1) && statusDropdown_1 == LinkStatus.New.description){
              projectCollection.saveCuttedProjectLinks(projectCollection.getTmpDirty(), LinkStatus.Unchanged.value, LinkStatus.New.value);
            }
            else if(_.isUndefined(statusDropdown_1)){
              projectCollection.saveProjectLinks(projectCollection.getTmpDirty(), LinkStatus.Unchanged.value);
            }
            break;
          }
          case LinkStatus.New.description : {
            if(!_.isUndefined(statusDropdown_1) && statusDropdown_1 == LinkStatus.Unchanged.description){
              projectCollection.saveCuttedProjectLinks(projectCollection.getTmpDirty(), LinkStatus.New.value, LinkStatus.Unchanged.value);
            }

            else if(!_.isUndefined(statusDropdown_1) && statusDropdown_1 == LinkStatus.Transfer.description){
              projectCollection.saveCuttedProjectLinks(projectCollection.getTmpDirty(), LinkStatus.New.value, LinkStatus.Transfer.value);
            }
            else if(_.isUndefined(statusDropdown_1)) {
              projectCollection.saveProjectLinks(projectCollection.getTmpDirty(), LinkStatus.New.value);
            }
            break;
          }
          case LinkStatus.Transfer.description : {
            if(!_.isUndefined(statusDropdown_1) && statusDropdown_1 == LinkStatus.New.description){
              projectCollection.saveCuttedProjectLinks(projectCollection.getTmpDirty(), LinkStatus.Unchanged.value, LinkStatus.New.value);
            }
            else if(_.isUndefined(statusDropdown_1)){
              projectCollection.saveProjectLinks(projectCollection.getTmpDirty(), LinkStatus.Transfer.value);
            }
            break;
          }
          case LinkStatus.Numbering.description : {
            projectCollection.saveProjectLinks(projectCollection.getTmpDirty(), LinkStatus.Numbering.value); break;
          }
          case LinkStatus.Terminated.description: {
            projectCollection.saveProjectLinks(projectCollection.getTmpDirty(), LinkStatus.Terminated.value); break;
          }
          case LinkStatus.Revert.description : {
            projectCollection.revertChangesRoadlink(selectedProjectLink); break;
          }
        }
        selectedProjectLinkProperty.setDirty(false);
        rootElement.html(emptyTemplate(currentProject.project));
      };

      var cancelChanges = function() {
        selectedProjectLinkProperty.setDirty(false);
        if(projectCollection.isDirty()) {
          projectCollection.revertLinkStatus();
          projectCollection.setDirty([]);
          projectCollection.setTmpDirty([]);
          projectLinkLayer.clearHighlights();
          $('.wrapper').remove();
          eventbus.trigger('roadAddress:projectLinksEdited');
          eventbus.trigger('roadAddressProject:toggleEditingRoad', true);
          eventbus.trigger('roadAddressProject:reOpenCurrent');
        } else {
          eventbus.trigger('roadAddress:openProject', projectCollection.getCurrentProject());
          eventbus.trigger('roadLinks:refreshView');
        }
      };

      rootElement.on('change', '#endDistance', function(eventData){
        var changedValue = parseInt(eventData.target.value);
        if(!isNaN(changedValue) && !isNaN(parseInt(endDistanceOriginalValue)) && changedValue !== endDistanceOriginalValue)
          $('#manualCPWarning').css('display', 'inline-block');
        else $('#manualCPWarning').css('display', 'none');
      });

      rootElement.on('click', '.split-form button.update', function() {
        eventbus.trigger('roadAddressProject:toggleEditingRoad', true);
        saveChanges();
      });

      rootElement.on('change', '#roadAddressProjectFormCut #dropdown_0, #roadAddressProjectFormCut #dropdown_1', function(){
        if (this.value == LinkStatus.New.description) {
          $('#drop_0_'+ LinkStatus.Revert.description).prop('disabled',true).prop('hidden',true);
          $('#drop_0_'+ LinkStatus.Transfer.description).prop('disabled',false).prop('hidden',false);
          $('#drop_0_'+ LinkStatus.Unchanged.description).prop('disabled',false).prop('hidden',false);
          $('#drop_0_'+ LinkStatus.New.description).prop('disabled',false).prop('hidden',true);
          $('#drop_1_'+ LinkStatus.New.description).prop('disabled',false).prop('hidden',true);
          $('#drop_1_'+ LinkStatus.Unchanged.description).prop('disabled',false).prop('hidden',false);
          $('#drop_1_'+ LinkStatus.Transfer.description).prop('disabled',false).prop('hidden',false);
          $('#drop_1_'+ LinkStatus.Revert.description).prop('disabled',true).prop('hidden',true);

        } else if ((this.value == LinkStatus.Unchanged.description )) {
          $('#drop_0_'+ LinkStatus.Revert.description).prop('disabled',true).prop('hidden',true);
          $('#drop_0_'+ LinkStatus.Transfer.description).prop('disabled',false).prop('hidden',false);
          $('#drop_0_'+ LinkStatus.Unchanged.description).prop('disabled',false).prop('hidden',true);
          $('#drop_0_'+ LinkStatus.New.description).prop('disabled',false).prop('hidden',false);
          $('#drop_1_'+ LinkStatus.New.description).prop('disabled',false).prop('hidden',false);
          $('#drop_1_'+ LinkStatus.Unchanged.description).prop('disabled',false).prop('hidden',true);
          $('#drop_1_'+ LinkStatus.Transfer.description).prop('disabled',false).prop('hidden',false);
          $('#drop_1_'+ LinkStatus.Revert.description).prop('disabled',true).prop('hidden',true);
        }
        else if ((this.value == LinkStatus.Transfer.description )) {
          $('#drop_0_' + LinkStatus.Revert.description).prop('disabled', true).prop('hidden', true);
          $('#drop_0_' + LinkStatus.Transfer.description).prop('disabled', false).prop('hidden', true);
          $('#drop_0_' + LinkStatus.Unchanged.description).prop('disabled', false).prop('hidden', true);
          $('#drop_0_' + LinkStatus.New.description).prop('disabled', false).prop('hidden', false);
          $('#drop_1_' + LinkStatus.New.description).prop('disabled', false).prop('hidden', false);
          $('#drop_1_' + LinkStatus.Unchanged.description).prop('disabled', false).prop('hidden', true);
          $('#drop_1_' + LinkStatus.Transfer.description).prop('disabled', false).prop('hidden', true);
          $('#drop_1_' + LinkStatus.Revert.description).prop('disabled', true).prop('hidden', true);
        }
      });

      rootElement.on('change', '#roadAddressProjectFormCut #dropdown_0, #roadAddressProjectFormCut #dropdown_1', function() {
        selectedProjectLinkProperty.setDirty(true);
        eventbus.trigger('roadAddressProject:toggleEditingRoad', false);
        $('#ajr').prop('disabled',false);
        $('#discontinuityDropdown').prop('disabled',false);
        $('#roadTypeDropDown').prop('disabled',false);

        if(this.value == LinkStatus.New.description){
          rootElement.find('.new-road-address').prop("hidden", false);
          projectCollection.setTmpDirty(_.filter(projectCollection.getTmpDirty(), function (l) { return l.status !== LinkStatus.Terminated.value;}).concat(selectedProjectLink));
        }
        if(this.value == LinkStatus.Unchanged.description){
          projectCollection.setDirty(projectCollection.getDirty().concat(_.map(selectedProjectLink, function (link) {
            return {'linkId': link.linkId, 'status': LinkStatus.Unchanged.value, 'roadLinkSource': link.roadLinkSource, 'points': link.points};
          })));
          projectCollection.setTmpDirty(projectCollection.getTmpDirty().concat(selectedProjectLink));
        }
        else if(this.value == LinkStatus.Transfer.description) {
          projectCollection.setDirty(_.filter(projectCollection.getDirty(), function(dirty) {return dirty.status !== LinkStatus.Unchanged.value;}).concat(_.map(selectedProjectLink, function (link) {
            return {'linkId': link.linkId, 'status': LinkStatus.Transfer.value, 'roadLinkSource': link.roadLinkSource, 'points': link.points};
          })));
          projectCollection.setTmpDirty(projectCollection.getDirty());
          rootElement.find('.new-road-address').prop("hidden", false);
        }
        else if(this.value == LinkStatus.Revert.description) {
          rootElement.find('.split-form button.update').prop("disabled", false);
        }
      });

      rootElement.on('change', '.split-form-group', function() {
        rootElement.find('.action-selected-field').prop("hidden", false);
      });

      rootElement.on('click', ' .split-form button.cancelLink', function(){
          if(applicationModel.getSelectedTool() == "Cut") {
              cancelChanges();
          }
      });

      rootElement.on('click', '.split-form button.send', function(){
        projectCollection.publishProject();
      });

      rootElement.on('click', '.split-form button.show-changes', function(){
        $(this).empty();
        projectChangeTable.show();
        var publishButton = sendRoadAddressChangeButton();
        var projectChangesButton = showProjectChangeButton();
        if(isProjectPublishable()) {
          $('#information-content').html('' +
              '<div class="form form-horizontal">' +
              '<p>' + 'Validointi ok. Voit tehdä tieosoitteenmuutosilmoituksen' + '<br>' +
              'tai jatkaa muokkauksia.' + '</p>' +
              '</div>');
          $('footer').html(publishButton);
        }
        else
          $('footer').html(projectChangesButton);
      });

      rootElement.on('keyup','.split-form-control.small-input', function () {
        checkInputs();
      });

    };
    bindEvents();
  };
})(this);

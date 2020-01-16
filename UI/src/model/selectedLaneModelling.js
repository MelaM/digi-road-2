(function(root) {
  root.SelectedLaneModelling = function(backend, collection, typeId, singleElementEventCategory, multiElementEventCategory, isSeparableAssetType) {
    var selection = [];
    var self = this;
    var dirty = false;
    var originalLinearAssetValue = null;
    var isSeparated = false;
    var isValid = true;
    var multipleSelected;

    var getLane = function (laneNumber) {
        _.find(selection, function (lane){
          return lane.laneCode == laneNumber;
        });
    };

    var singleElementEvent = function(eventName) {
      return singleElementEventCategory + ':' + eventName;
    };

    var multiElementEvent = function(eventName) {
      return multiElementEventCategory + ':' + eventName;
    };

    this.splitLinearAsset = function(id, split) {
      collection.splitLinearAsset(id, split, function(splitLinearAssets) {
        selection = [splitLinearAssets.created, splitLinearAssets.existing];
        originalLinearAssetValue = splitLinearAssets.existing.value;
        dirty = true;
        collection.setSelection(self);
        eventbus.trigger(singleElementEvent('selected'), self);
      });
    };

    this.separate = function() {
      selection = collection.separateLinearAsset(_.head(selection));
      isSeparated = true;
      dirty = true;
      eventbus.trigger(singleElementEvent('separated'), self);
      eventbus.trigger(singleElementEvent('selected'), self);
    };

    //need to fetch the link id
    this.open = function(linearAsset, singleLinkSelect) {
      multipleSelected = false;
      self.close();
      // selection = singleLinkSelect ? [linearAsset] : collection.getGroup(linearAsset);
      // originalLinearAssetValue = self.getValue();
      // collection.setSelection(self);
      backend.getLanesByLinkId(linearAsset.linkId, function(asset) {
        originalLinearAssetValue = asset;
        selection = asset;
        collection.setSelection(self);
        eventbus.trigger(singleElementEvent('selected'), self);
      });
    };

    this.getLinearAsset = function(id) {
      return collection.getById(id);
    };

    this.addSelection = function(linearAssets){
      var partitioned = _.groupBy(linearAssets, isUnknown);
      var existingLinearAssets = _.uniq(partitioned[false] || [], 'id');
      var unknownLinearAssets = _.uniq(partitioned[true] || [], 'generatedId');
      selection = selection.concat(existingLinearAssets.concat(unknownLinearAssets));
    };

    this.removeSelection = function(linearAssets){
      selection = _.filter(selection, function(asset){
        if(isUnknown(asset))
          return !_.some(linearAssets, function(iasset){ return iasset.generatedId === asset.generatedId;});

        return !_.some(linearAssets, function(iasset){ return iasset.id === asset.id;});
      });
    };

    this.openMultiple = function(linearAssets) {
      multipleSelected = true;
      var partitioned = _.groupBy(linearAssets, isUnknown);
      var existingLinearAssets = _.uniq(partitioned[false] || [], 'id');
      var unknownLinearAssets = _.uniq(partitioned[true] || [], 'generatedId');
      selection = existingLinearAssets.concat(unknownLinearAssets);
      eventbus.trigger(singleElementEvent('multiSelected'));
    };

    this.close = function() {
      if (!_.isEmpty(selection) && !dirty) {
        eventbus.trigger(singleElementEvent('unselect'), self);
        collection.setSelection(null);
        selection = [];
      }
    };

    this.closeMultiple = function() {
      eventbus.trigger(singleElementEvent('unselect'), self);
      dirty = false;
      collection.setSelection(null);
      selection = [];
    };

    this.saveMultiple = function(value) {
      eventbus.trigger(singleElementEvent('saving'));
      var partition = _.groupBy(_.map(selection, function(item){ return _.omit(item, 'geometry'); }), isUnknown);
      var unknownLinearAssets = partition[true];
      var knownLinearAssets = partition[false];

      var payload = {
        newLimits: _.map(unknownLinearAssets, function(x) { return _.merge(x, {value: value, expired: false }); }),
        ids: _.map(knownLinearAssets, 'id'),
        value: value,
        typeId: typeId
      };
      var backendOperation = _.isUndefined(value) ? backend.deleteLinearAssets : backend.createLinearAssets;
      backendOperation(payload, function() {
        dirty = false;
        self.closeMultiple();
        eventbus.trigger(multiElementEvent('massUpdateSucceeded'), selection.length);
      }, function() {
        eventbus.trigger(multiElementEvent('massUpdateFailed'), selection.length);
      });
    };

    var saveSplit = function() {
      eventbus.trigger(singleElementEvent('saving'));
      collection.saveSplit(function() {
        dirty = false;
        self.close();
      });
    };

    var saveSeparation = function() {
      eventbus.trigger(singleElementEvent('saving'));
      collection.saveSeparation(function() {
        dirty = false;
        isSeparated = false;
        self.close();
      });
    };

    var saveExisting = function() {
      eventbus.trigger(singleElementEvent('saving'));
      var payloadContents = function() {
        if (self.isUnknown()) {
          return { newLimits: _.map(selection, function(item){ return _.omit(item, 'geometry'); }) };
        } else {
          return { ids: _.map(selection, 'id') };
        }
      };
      var payload = _.merge({value: self.getValue(), typeId: typeId}, payloadContents());
      var backendOperation = _.isUndefined(self.getValue()) ? backend.deleteLinearAssets : backend.createLinearAssets;

      backendOperation(payload, function() {
        dirty = false;
        self.close();
        eventbus.trigger(singleElementEvent('saved'));
      }, function() {
        eventbus.trigger('asset:updateFailed');
      });
    };

    var isUnknown = function(linearAsset) {
      return !_.has(linearAsset, 'id');
    };

    this.isUnknown = function(laneNumber) {
      return isUnknown(_.find(selection, function (lane){
        return lane.laneCode == laneNumber;
      }));
    };

    this.isSplit = function(laneNumber) {
      var lane = _.find(selection, function (lane){
        return lane.laneCode == laneNumber;
      });

      return !isSeparated && !_.isEmpty(lane) && lane.id === null;
    };

    this.isSeparated = function() {
      return isSeparated;
    };

    this.isSplitOrSeparated = function() {
      return this.isSplit() || this.isSeparated();
    };

    this.save = function() {
      if (self.isSplit()) {
        saveSplit();
      } else if (isSeparated) {
        saveSeparation();
      } else {
        saveExisting();
      }
    };

    var cancelCreation = function() {
      if (isSeparated) {
        var originalLinearAsset = _.cloneDeep(selection[0]);
        originalLinearAsset.value = originalLinearAssetValue;
        originalLinearAsset.sideCode = 1;
        collection.replaceSegments([selection[0]], [originalLinearAsset]);
      }
      collection.setSelection(null);
      selection = [];
      dirty = false;
      isSeparated = false;
      collection.cancelCreation();
      eventbus.trigger(singleElementEvent('unselect'), self);
    };

    var cancelExisting = function() {
      var newGroup = _.map(selection, function(s) { return _.assign({}, s, { value: originalLinearAssetValue }); });
      selection = collection.replaceSegments(selection, newGroup);
      dirty = false;
      eventbus.trigger(singleElementEvent('cancelled'), self);
    };

    this.cancel = function() {
      if (self.isSplit() || self.isSeparated()) {
        cancelCreation();
      } else {
        cancelExisting();
      }
      self.close();
    };

    this.verify = function() {
      eventbus.trigger(singleElementEvent('saving'));
      var knownLinearAssets = _.reject(selection, isUnknown);
      var payload = {ids: _.map(knownLinearAssets, 'id'), typeId: typeId};
      collection.verifyLinearAssets(payload);
      dirty = false;
      self.close();
    };

    this.exists = function() {
      return !_.isEmpty(selection);
    };

    var getProperty = function(lane, propertyName) {
      return _.has(lane, propertyName) ? lane[propertyName] : null;
    };

    var getPropertyB = function(propertyName) {
      return _.has(selection[1], propertyName) ? selection[1][propertyName] : null;
    };

    this.getId = function() {
      return getProperty('id');
    };

    this.getValue = function() {
      var value = getProperty('value');
      return _.isNull(value) ? undefined : value;
    };

    this.getBValue = function() {
      var value = getPropertyB('value');
      return _.isNull(value) ? undefined : value;
    };

    this.getModifiedBy = function() {
      return dateutil.extractLatestModifications(selection, 'modifiedAt').modifiedBy;
    };

    this.getModifiedDateTime = function() {
      return dateutil.extractLatestModifications(selection, 'modifiedAt').modifiedAt;
    };

    this.getCreatedBy = function(laneNumber) {
      var lane = getLane(laneNumber);
      return getProperty(lane,'createdBy');
    };

    this.getCreatedDateTime = function() {
      return selection.length === 1 ? getProperty('createdAt') : null;
    };

    this.getAdministrativeClass = function() {
      var value = getProperty('administrativeClass');
      return _.isNull(value) ? undefined : value;
    };

    this.getVerifiedBy = function() {
      return selection.length === 1 ? getProperty('verifiedBy') : null;
    };

    this.getVerifiedDateTime = function() {
      return selection.length === 1 ? getProperty('verifiedAt') : null;
    };

    this.get = function() {
      return selection;
    };

    this.count = function() {
      return selection.length;
    };

    this.setValue = function(laneNumber, value) {
      var lane = _.find(selection, function (lane){
        return lane.laneCode == laneNumber;
      });

      var laneIndex = _.findIndex(selection, function (lane){
        return lane.laneCode == laneNumber;
      });

      var newGroup = _.map(lane, function(s) { return _.assign({}, s, value); });
      selection[laneIndex].propertyData = collection.replaceSegments(selection, lane, newGroup);
      dirty = true;
      eventbus.trigger(singleElementEvent('valueChanged'), self, multipleSelected);

    };

    this.setMultiValue = function(value) {
      var newGroup = _.map(selection, function(s) { return _.assign({}, s, { value: value }); });
      selection = collection.replaceSegments(selection, newGroup);
      eventbus.trigger(multiElementEvent('valueChanged'), self, multipleSelected);
    };

    function isValueDifferent(selection){
      if(selection.length == 1) return true;

      var nonEmptyValues = _.map(selection, function (select) {
        return  _.filter(select.value, function(val){ return !_.isEmpty(val.value); });
      });
      var zipped = _.zip(nonEmptyValues[0], nonEmptyValues[1]);
      var mapped = _.map(zipped, function (zipper) {
        if(!zipper[1] || !zipper[0])
          return true;
        else
          return zipper[0].value !== zipper[1].value;
      });
      return _.includes(mapped, true);
    }

    function getRequiredFields(properties){
      return _.filter(properties, function (property) {
        return (property.publicId === "huoltotie_kayttooikeus") || (property.publicId === "huoltotie_huoltovastuu");
      });
    }

    function checkFormMandatoryFields(formSelection) {
      if (_.isUndefined(formSelection.value)) return true;
      var requiredFields = getRequiredFields(formSelection.value);
      return !_.some(requiredFields, function(fields){ return fields.value === ''; });
    }

    function checkFormsMandatoryFields(formSelections) {
      var mandatorySelected = !_.some(formSelections, function(formSelection){ return !checkFormMandatoryFields(formSelection); });
      return mandatorySelected;
    }

    this.setAValue = function (value) {
      if (value != selection[0].value) {
        var newGroup = _.assign({}, selection[0], { value: value });
        selection[0] = collection.replaceCreatedSplit(selection[0], newGroup);
        eventbus.trigger(singleElementEvent('valueChanged'), self);
      }
    };

    this.setBValue = function (value) {
      if (value != selection[1].value) {
        var newGroup = _.assign({}, selection[1], { value: value });
        selection[1] = collection.replaceExistingSplit(selection[1], newGroup);
        eventbus.trigger(singleElementEvent('valueChanged'), self);
      }
    };

    this.removeValue = function() {
      self.setValue(undefined);
    };

    this.removeMultiValue = function() {
      self.setMultiValue();
    };

    this.removeAValue = function() {
      self.setAValue(undefined);
    };

    this.removeBValue = function() {
      self.setBValue(undefined);
    };

    this.isDirty = function() {
      return dirty;
    };

    this.setDirty = function(dirtyValue) {
      dirty = dirtyValue;
    };

    this.isSelected = function(linearAsset) {
      return _.some(selection, function(selectedLinearAsset) {
        return isEqual(linearAsset, selectedLinearAsset);
      });
    };

    this.isSeparable = function() {
      return isSeparableAssetType &&
        getProperty('sideCode') === validitydirections.bothDirections &&
        getProperty('trafficDirection') === 'BothDirections' &&
        !self.isSplit() &&
        selection.length === 1;
    };

    this.isSaveable = function() {
      var valuesDiffer = function () { return (selection[0].value !== selection[1].value); };
      if (this.isDirty()) {
            if (this.isSplitOrSeparated() && valuesDiffer())
              return true;

            if (!this.isSplitOrSeparated())
              return true;
      }
      return false;
    };

    var isEqual = function(a, b) {
      return (_.has(a, 'generatedId') && _.has(b, 'generatedId') && (a.generatedId === b.generatedId)) ||
        ((!isUnknown(a) && !isUnknown(b)) && (a.id === b.id));
    };

    this.requiredPropertiesMissing = function (formStructure) {

      var requiredFields = _.filter(formStructure.fields, function(form) { return form.required; });

      var assets = this.isSplitOrSeparated() ? _.filter(selection, function(asset){ return asset.value; }) : selection;

      return !_.every(assets, function(asset){

        return _.every(requiredFields, function(field){
          if(!asset.value || _.isEmpty(asset.value))
            return false;

          var property  = _.find(asset.value.properties, function(p){ return p.publicId === field.publicId;});

          if(!property)
            return false;

          if(_.isEmpty(property.values))
            return false;

          return _.some(property.values, function(value){ return value && !_.isEmpty(value.value); });
        });
      });
    };

    this.isSplitOrSeparatedEqual = function(){
      if (_.filter(selection, function(p){return p.value;}).length <= 1)
        return false;

      return _.every(selection[0].value.properties, function(property){
        var iProperty =  _.find(selection[1].value.properties, function(p){ return p.publicId === property.publicId; });
        if(!iProperty)
          return false;

        return _.isEqual(property.values, iProperty.values);
      });
    };

    this.hasValidValues = function () {
      return isValid;
    };

    this.setValidValues = function (valid) {
      isValid = valid;
    };
  };
})(this);

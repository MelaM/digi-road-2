(function(root) {
  root.GroupedPointAssetLayer = function(params) {
    var roadLayer = params.roadLayer,
      application = applicationModel,
      collection = params.collection,
      map = params.map,
      roadCollection = params.roadCollection,
      style = params.style,
      selectedAsset = params.selectedAsset,
      mapOverlay = params.mapOverlay,
      layerName = params.layerName,
      roadAddressInfoPopup = params.roadAddressInfoPopup,
      assetLabel = params.assetLabel,
      allowGrouping = params.allowGrouping,
      assetGrouping = params.assetGrouping,
      authorizationPolicy = params.authorizationPolicy;
    var pointAssetLayerStyles = PointAssetLayerStyles(params.roadLayer);

    Layer.call(this, layerName, roadLayer);
    var me = this;
    me.minZoomForContent = zoomlevels.minZoomForAssets;
    var extraEventListener = _.extend({running: false}, eventbus);
    var vectorSource = new ol.source.Vector();
    var vectorLayer = new ol.layer.Vector({
      source : vectorSource,
      style : function(feature){
        return style.browsingStyleProvider.getStyle(feature);
      },
      renderBuffer: 300
    });
    vectorLayer.set('name', layerName);
    vectorLayer.setOpacity(1);
    vectorLayer.setVisible(true);
    map.addLayer(vectorLayer);

    var selectControl = new SelectToolControl(application, vectorLayer, map, false,{
      style : function (feature) {
        return style.browsingStyleProvider.getStyle(feature);
      },
      onSelect : pointAssetOnSelect,
      draggable : false,
      filterGeometry : function(feature){
        return feature.getGeometry() instanceof ol.geom.Point;
      }
    });

    function pointAssetOnSelect(evt) {
      if(evt.selected.length > 0 && evt.deselected.length === 0){
        var feature = evt.selected[0];
        var properties = feature.getProperties();
        var administrativeClass = obtainAdministrativeClass(properties);
        var asset = _.merge({}, properties, {administrativeClass: administrativeClass});
        selectedAsset.open(asset);
      }
      else {
        if(evt.deselected.length > 0 && !selectedAsset.isDirty()) {
          selectedAsset.close();
        }else{
          applySelection();
        }
      }
    }

    this.selectControl = selectControl;

    function createFeature(asset) {
      var rotation = determineRotation(asset);
      var bearing = determineBearing(asset);
      var administrativeClass = obtainAdministrativeClass(asset);
      var feature =  new ol.Feature({geometry : new ol.geom.Point([asset.lon, asset.lat])});
      var obj = _.merge({}, asset, {rotation: rotation, bearing: bearing, administrativeClass: administrativeClass}, feature.getProperties());
      feature.setProperties(obj);
      return feature;
    }

    function determineRotation(asset) {
      var rotation = 0;
      if (!asset.floating && asset.geometry && asset.geometry.length > 0){
        var bearing = determineBearing(asset);
        rotation = validitydirections.calculateRotation(bearing, asset.validityDirection);
      }
      return rotation;
    }

    function determineBearing(asset) {
      var bearing = 90;
      if (!asset.floating && asset.geometry && asset.geometry.length > 0){
        var nearestLine = geometrycalculator.findNearestLine([{ points: asset.geometry }], asset.lon, asset.lat);
        bearing = geometrycalculator.getLineDirectionDegAngle(nearestLine);
      }
      return bearing;
    }

    this.refreshView = function() {
      eventbus.once('roadLinks:fetched', function () {
        var roadLinks = roadCollection.getAll();
        roadLayer.drawRoadLinks(roadLinks, zoomlevels.getViewZoom(map));
        me.drawOneWaySigns(roadLayer.layer, roadLinks);
        selectControl.activate();
      });
      if(collection.complementaryIsActive())
        roadCollection.fetchWithComplementary(map.getView().calculateExtent(map.getSize()));
      else
        roadCollection.fetch(map.getView().calculateExtent(map.getSize()));
      collection.fetch(map.getView().calculateExtent(map.getSize())).then(function(assets) {
        if (selectedAsset.exists()) {
          var assetsWithoutSelectedAsset = _.reject(assets, {id: selectedAsset.getId()});
          assets = assetsWithoutSelectedAsset.concat([selectedAsset.get()]);
        }

        if (me.isStarted()) {
          withDeactivatedSelectControl(function() {
            me.removeLayerFeatures();
          });
          var features = (!allowGrouping) ? _.map(assets, createFeature) : getGroupedFeatures(assets);
          selectControl.clear();
          vectorLayer.getSource().addFeatures(features);
          if(assetLabel)
            vectorLayer.getSource().addFeatures(assetLabel.renderFeaturesByPointAssets(assets, zoomlevels.getViewZoom(map)));
          applySelection();
        }
      });
    };

    this.stop = function() {
      if (me.isStarted()) {
        me.removeLayerFeatures();
        me.deactivateSelection();
        me.eventListener.stopListening(eventbus);
        me.eventListener.running = false;
        handleUnSelected();
      }
    };

    var getGroupedFeatures = function (assets) {
      var assetGroups = assetGrouping.groupByDistance(assets, zoomlevels.getViewZoom(map));
      var modifiedAssets = _.forEach(assetGroups, function (assetGroup) {
        _.map(assetGroup, function (asset) {
          asset.lon = _.head(assetGroup).lon;
          asset.lat = _.head(assetGroup).lat;
        });
      });
      return _.map(_.flatten(modifiedAssets), createFeature);
    };

    function obtainAdministrativeClass(asset){
      return selectedAsset.getAdministrativeClass(asset.linkId);
    }

    this.removeLayerFeatures = function() {
      vectorLayer.getSource().clear();
    };

    function applySelection() {
      if (selectedAsset.exists()) {
        var asset = selectedAsset.get();
        var feature = _.filter(vectorLayer.getSource().getFeatures(), function(feature) {
          return selectedAsset.isSelected(feature.getProperties()) &&  feature.getProperties().lon === asset.lon && feature.getProperties().lat === asset.lat;
        });
        if (feature) {
          selectControl.addSelectionFeatures(feature);
        }
      }
    }

    function withDeactivatedSelectControl(f) {
      var isActive = me.selectControl.active;
      if (isActive) {
        selectControl.deactivate();
        f();
        selectControl.activate();
      } else {
        f();
      }
    }

    this.layerStarted = function(eventListener) {
      bindEvents(eventListener);
      showRoadLinkInformation();
    };

    function bindEvents(eventListener) {
      eventListener.listenTo(eventbus, 'map:clicked', handleMapClick);
      eventListener.listenTo(eventbus, layerName + ':saved ' + layerName + ':cancelled', handleSavedOrCancelled);
      eventListener.listenTo(eventbus, layerName + ':selected', handleSelected);
      eventListener.listenTo(eventbus, layerName + ':unselected', handleUnSelected);
      eventListener.listenTo(eventbus, layerName + ':changed', handleChanged);
      eventListener.listenTo(eventbus, 'toggleWithRoadAddress', refreshSelectedView);
    }

    var startListeningExtraEvents = function(){
      extraEventListener.listenTo(eventbus, layerName+'-complementaryLinks:show', showWithComplementary);
      extraEventListener.listenTo(eventbus, layerName+'-complementaryLinks:hide', hideComplementary);
    };

    var stopListeningExtraEvents = function(){
      extraEventListener.stopListening(eventbus);
    };

    function handleSelected() {
      applySelection();
    }

    function handleUnSelected() {
      selectControl.clear();
    }

    function handleSavedOrCancelled() {
      mapOverlay.hide();
      me.activateSelection();
      roadLayer.clearSelection();
      me.refreshView();
    }

    function handleChanged() {
      var asset = selectedAsset.get();
      var newAsset = _.merge({}, asset, {rotation: determineRotation(asset), bearing: determineBearing(asset), administrativeClass: obtainAdministrativeClass(asset)});
      _.find(vectorLayer.getSource().getFeatures(), {values_: {id: newAsset.id}}).values_= newAsset;
      var featureRedraw = _.find(vectorLayer.getSource().getFeatures(), function(feature) {
        return feature.getProperties().id === newAsset.id;
      });
      featureRedraw.setProperties({'geometry': new ol.geom.Point([newAsset.lon, newAsset.lat])});
      selectControl.addSelectionFeatures([featureRedraw]);

    }

    function handleMapClick() {
      if (selectedAsset.isDirty()) {
        me.displayConfirmMessage();
      }
    }

    function showWithComplementary() {
      collection.activeComplementary(true);
      me.refreshView();
    }

    function show(map) {
      startListeningExtraEvents();
      vectorLayer.setVisible(true);
      roadAddressInfoPopup.start();
      me.refreshView();
      me.show(map);
    }

    function hideComplementary() {
      collection.activeComplementary(false);
      selectedAsset.close();
      me.refreshView();
    }

    function hide() {
      selectedAsset.close();
      vectorLayer.setVisible(false);
      roadAddressInfoPopup.stop();
      stopListeningExtraEvents();
      me.stop();
      me.hide();
    }

    function excludeRoadByAdminClass(roadCollection) {
      return _.filter(roadCollection, function (roads) {
        return !authorizationPolicy.formEditModeAccess(selectedAsset, roads.linkId);
      });
    }

    var refreshSelectedView = function(){
      if(applicationModel.getSelectedLayer() == layerName)
        me.refreshView();
    };

    function showRoadLinkInformation() {
      if(params.showRoadLinkInfo) {
        roadLayer.setLayerSpecificStyleProvider(params.layerName, function() {
          return pointAssetLayerStyles;
        });
      }
    }

    return {
      show: show,
      hide: hide,
      minZoomForContent: me.minZoomForContent
    };
  };
})(this);

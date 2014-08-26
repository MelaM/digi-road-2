window.AssetLayer = function(map, roadCollection) {
  var eventListener = _.extend({running: false}, eventbus);
  var selectedAsset;
  var assetDirectionLayer = new OpenLayers.Layer.Vector('assetDirection');
  var assetLayer = new OpenLayers.Layer.Boxes('asset');

  map.addLayer(assetDirectionLayer);
  map.addLayer(assetLayer);

  var overlay;
  var selectedControl = 'Select';

  var clickTimestamp;
  var clickCoords;
  var assetIsMoving = false;


  var hideAsset = function(asset) {
    assetDirectionLayer.destroyFeatures(asset.massTransitStop.getDirectionArrow());
    asset.massTransitStop.getMarker().display(false);
  };

  var showAsset = function(asset) {
    asset.massTransitStop.getMarker().display(true);
    assetDirectionLayer.addFeatures(asset.massTransitStop.getDirectionArrow());
  };

  var mouseUpHandler = function(asset) {
    clickTimestamp = null;
    unregisterMouseUpHandler(asset);
    if (assetIsMoving) {
      eventbus.trigger('asset:moved', {
        lon: selectedAssetModel.get('lon'),
        lat: selectedAssetModel.get('lat'),
        bearing: selectedAssetModel.get('bearing'),
        roadLinkId: selectedAssetModel.get('roadLinkId')
      });
      assetIsMoving = false;
    }
  };

  var mouseUp = function(asset) {
    return function(evt) {
      OpenLayers.Event.stop(evt);
      mouseUpHandler(asset);
    };
  };

  var mouseDown = function(asset) {
    return function(evt) {
      var commenceAssetDragging = function() {
        clickTimestamp = new Date().getTime();
        clickCoords = [evt.clientX, evt.clientY];
        OpenLayers.Event.stop(evt);
        selectedAsset = asset;
        registerMouseUpHandler(asset);
        setInitialClickOffsetFromMarkerBottomLeft(evt.clientX, evt.clientY);
      };

      if (selectedControl === 'Select') {
        if (selectedAssetModel.getId() === asset.data.id) {
          commenceAssetDragging();
        }
      }
    };
  };

  var createMouseClickHandler = function(asset) {
    return function() {
      var selectAsset = function() {
        selectedAssetModel.change(asset.data);
      };

      if (selectedControl === 'Select') {
        if (selectedAssetModel.getId() !== asset.data.id) {
          if (selectedAssetModel.isDirty()) {
            new Confirm();
          } else {
            selectAsset();
          }
        }
      }
    };
  };

  var setInitialClickOffsetFromMarkerBottomLeft = function(mouseX, mouseY) {
    var markerPosition = $(selectedAsset.massTransitStop.getMarker().div).offset();
    initialClickOffsetFromMarkerBottomleft.x = mouseX - markerPosition.left;
    initialClickOffsetFromMarkerBottomleft.y = mouseY - markerPosition.top;
  };

  var registerMouseUpHandler = function(asset) {
    var mouseUpFn = mouseUp(asset);
    asset.mouseUpHandler = mouseUpFn;
    map.events.register('mouseup', map, mouseUpFn, true);
  };

  var unregisterMouseUpHandler = function(asset) {
    map.events.unregister('mouseup', map, asset.mouseUpHandler);
    asset.mouseUpHandler = null;
  };

  var registerMouseDownHandler = function(asset) {
    var mouseDownFn = mouseDown(asset);
    asset.mouseDownHandler = mouseDownFn;
    asset.massTransitStop.getMarker().events.register('mousedown', assetLayer, mouseDownFn);
  };

  var unregisterMouseDownHandler = function(asset) {
    asset.massTransitStop.getMarker().events.unregister('mousedown', assetLayer, asset.mouseDownHandler);
    asset.mouseDownHandler = null;
  };

  var createAsset = function(assetData) {
    var massTransitStop = new MassTransitStop(assetData, map);
    assetDirectionLayer.addFeatures(massTransitStop.getDirectionArrow(true));
    var marker = massTransitStop.getMarker(true);
    var asset = {};
    asset.data = assetData;
    asset.massTransitStop = massTransitStop;
    var mouseClickHandler = createMouseClickHandler(asset);
    asset.mouseClickHandler = mouseClickHandler;
    marker.events.register('click', assetLayer, mouseClickHandler);
    return asset;
  };

  var setAssetVisibilityByValidityPeriod = function(asset) {
    if (assetsModel.selectedValidityPeriodsContain(asset.data.validityPeriod)) {
      showAsset(asset);
    } else {
      hideAsset(asset);
    }
  };

  var addAssetToLayers = function(asset) {
    assetLayer.addMarker(asset.massTransitStop.getMarker());
    assetDirectionLayer.addFeatures(asset.massTransitStop.getDirectionArrow());
  };

  var addAssetToLayersAndSetVisibility = function(asset) {
    addAssetToLayers(asset);
    setAssetVisibilityByValidityPeriod(asset);
  };

  var removeAssetFromMap = function(asset) {
    assetDirectionLayer.removeFeatures(asset.massTransitStop.getDirectionArrow());
    var marker = asset.massTransitStop.getMarker();
    assetLayer.removeMarker(marker);
  };

  var isSelected = function(asset) {
    return selectedAsset && selectedAsset.data.id === asset.id;
  };

  var convertBackendAssetToUIAsset = function(backendAsset, centroidLonLat, assetGroup) {
    var uiAsset = backendAsset;
    var lon = centroidLonLat.lon;
    var lat = centroidLonLat.lat;
    if (isSelected(uiAsset)) {
      lon = selectedAsset.data.lon;
      lat = selectedAsset.data.lat;
      uiAsset.lon = lon;
      uiAsset.lat = lat;
    }
    uiAsset.group = {
      lon: lon,
      lat: lat,
      assetGroup: assetGroup
    };
    return uiAsset;
  };

  var renderAssets = function(assetDatas) {
    assetLayer.setVisibility(true);
    _.each(assetDatas, function(assetGroup) {
      assetGroup = _.sortBy(assetGroup, 'id');
      var centroidLonLat = geometrycalculator.getCentroid(assetGroup);
      _.each(assetGroup, function(asset) {
        var uiAsset = convertBackendAssetToUIAsset(asset, centroidLonLat, assetGroup);
        if (!assetsModel.getAsset(uiAsset.id)) {
          var assetInModel = createAsset(uiAsset);
          assetsModel.insertAsset(assetInModel, uiAsset.id);
          addAssetToLayers(assetInModel);
        }
        setAssetVisibilityByValidityPeriod(assetsModel.getAsset(uiAsset.id));
        if (isSelected(uiAsset)) {
          selectedAsset = assetsModel.getAsset(uiAsset.id);
          selectedAsset.massTransitStop.select();
          registerMouseDownHandler(selectedAsset);
        }
      });
    });
  };

  var cancelCreate = function() {
    removeOverlay();
    removeAssetFromMap(selectedAsset);
  };

  var cancelUpdate = function(asset) {
    deselectAsset(selectedAsset);
    destroyAsset(asset);
    addNewAsset(asset);
    selectedAsset = regroupAssetIfNearOtherAssets(asset);
    registerMouseDownHandler(selectedAsset);
    selectedAsset.massTransitStop.select();
  };

  var updateAsset = function(asset) {
    removeAssetFromMap(selectedAsset);
    selectedAsset = addNewAsset(asset);
  };

  var handleValidityPeriodChanged = function() {
    _.each(assetsModel.getAssets(), function(asset) {
      if (assetsModel.selectedValidityPeriodsContain(asset.data.validityPeriod) && zoomlevels.isInAssetZoomLevel(map.getZoom())) {
        showAsset(asset);
      } else {
        hideAsset(asset);
      }
    });
    if (selectedAsset && selectedAsset.data.validityPeriod === undefined) {
      return;
    }

    if (selectedAsset && !assetsModel.selectedValidityPeriodsContain(selectedAsset.data.validityPeriod)) {
      closeAsset();
    }
  };

  function redrawGroup(group) {
    var groupAssets = group.assetGroup;
    _.each(groupAssets, function(asset) {
      var uiAsset = assetsModel.getAsset(asset.id);
      uiAsset.massTransitStop.rePlaceInGroup();
    });
  }

  var addAssetToGroup = function(asset, group) {
    var assetGroup = _.sortBy(group.assetGroup.concat([asset.data]), 'id');
    _.each(assetGroup, function(asset) {
      asset.group.assetGroup = assetGroup;
    });
  };

  function createAndGroupUIAsset(backendAsset) {
    var uiAsset;
    var assetToGroupWith = assetGrouping.findNearestAssetWithinGroupingDistance(_.values(assetsModel.getAssets()), backendAsset);
    if (assetToGroupWith) {
      uiAsset = createAsset(convertBackendAssetToUIAsset(backendAsset, assetToGroupWith.data.group, assetToGroupWith.data.group.assetGroup));
      assetsModel.insertAsset(uiAsset, uiAsset.data.id);
      addAssetToGroup(uiAsset, assetToGroupWith.data.group);
      redrawGroup(assetToGroupWith.data.group);
    } else {
      var group = createDummyGroup(backendAsset.lon, backendAsset.lat, backendAsset);
      uiAsset = createAsset(convertBackendAssetToUIAsset(backendAsset, group, group.assetGroup));
      assetsModel.insertAsset(uiAsset, uiAsset.data.id);
    }
    return uiAsset;
  }

  var handleAssetCreated = function(asset) {
    removeAssetFromMap(selectedAsset);
    deselectAsset(selectedAsset);

    var uiAsset = createAndGroupUIAsset(asset);
    addAssetToLayersAndSetVisibility(uiAsset);

    selectedAsset = uiAsset;
    selectedAsset.massTransitStop.select();
    registerMouseDownHandler(selectedAsset);
  };

  var handleAssetSaved = function(asset, positionUpdated) {
    _.merge(assetsModel.getAsset(asset.id).data, asset);
    if (positionUpdated) {
      redrawGroup(selectedAsset.data.group);
      destroyAsset(asset);
      deselectAsset(selectedAsset);

      var uiAsset = createAndGroupUIAsset(asset);
      addAssetToLayersAndSetVisibility(uiAsset);

      selectedAsset = uiAsset;
      selectedAsset.massTransitStop.select();
      registerMouseDownHandler(selectedAsset);
    }
  };

  var parseAssetDataFromAssetsWithMetadata = function(assets) {
    return _.chain(assets)
      .values()
      .pluck('data')
      .map(function(x) { return _.omit(x, 'group'); })
      .value();
  };

  var regroupAssetIfNearOtherAssets = function(asset) {
    var regroupedAssets = assetGrouping.groupByDistance(parseAssetDataFromAssetsWithMetadata(assetsModel.getAssets()), map.getZoom());
    var groupContainingSavedAsset = _.find(regroupedAssets, function(assetGroup) {
      var assetGroupIds = _.pluck(assetGroup, 'id');
      return _.contains(assetGroupIds, asset.id);
    });
    var assetIds = _.map(groupContainingSavedAsset, function(asset) { return asset.id.toString(); });

    if (groupContainingSavedAsset.length > 1) {
      assetsModel.destroyGroup(assetIds);
    }

    return assetsModel.getAsset(asset.id);
  };

  var reRenderGroup = function(destroyedAssets) {
    _.each(destroyedAssets, removeAssetFromMap);
    renderAssets([parseAssetDataFromAssetsWithMetadata(destroyedAssets)]);
  };

  var handleAssetPropertyValueChanged = function(propertyData) {
    var turnArrow = function(asset, direction) {
      assetDirectionLayer.destroyFeatures(asset.massTransitStop.getDirectionArrow());
      asset.massTransitStop.getDirectionArrow().style.rotation = direction;
      assetDirectionLayer.addFeatures(asset.massTransitStop.getDirectionArrow());
    };

    if (propertyData.propertyData.publicId === 'vaikutussuunta') {
      var validityDirection = propertyData.propertyData.values[0].propertyValue;
      selectedAsset.data.validityDirection = validityDirection;
      turnArrow(selectedAsset, validitydirections.calculateRotation(selectedAsset.data.bearing, validityDirection));
    }
  };

  var createNewAsset = function(lonlat) {
    var selectedLon = lonlat.lon;
    var selectedLat = lonlat.lat;
    var nearestLine = geometrycalculator.findNearestLine(roadCollection.getAll(), selectedLon, selectedLat);
    var projectionOnNearestLine = geometrycalculator.nearestPointOnLine(nearestLine, { x: selectedLon, y: selectedLat });
    var bearing = geometrycalculator.getLineDirectionDegAngle(nearestLine);
    var data = {
      bearing: bearing,
      validityDirection: validitydirections.sameDirection,
      lon: projectionOnNearestLine.x,
      lat: projectionOnNearestLine.y,
      roadLinkId: nearestLine.roadLinkId
    };
    data.group = createDummyGroup(projectionOnNearestLine.x, projectionOnNearestLine.y, data);
    var massTransitStop = new MassTransitStop(data);

    deselectAsset(selectedAsset);
    selectedAsset = {directionArrow: massTransitStop.getDirectionArrow(true),
      data: data,
      massTransitStop: massTransitStop};
    selectedAsset.data.imageIds = [];
    eventbus.trigger('asset:placed', selectedAsset.data);

    assetDirectionLayer.addFeatures(selectedAsset.massTransitStop.getDirectionArrow());
    assetLayer.addMarker(selectedAsset.massTransitStop.createNewMarker());

    var applyBlockingOverlays = function() {
      var overlay = Oskari.clazz.create('Oskari.userinterface.component.Overlay');
      overlay.overlay('#contentMap,#map-tools');
      overlay.followResizing(true);
      return overlay;
    };
    overlay = applyBlockingOverlays();
  };

  var removeOverlay = function() {
    if (overlay) {
      overlay.close();
    }
  };

  var addNewAsset = function(asset) {
    asset.group = createDummyGroup(asset.lon, asset.lat, asset);
    var uiAsset = createAsset(asset);
    assetsModel.insertAsset(uiAsset, asset.id);
    addAssetToLayersAndSetVisibility(assetsModel.getAsset(asset.id));
    return uiAsset;
  };

  var createDummyGroup = function(lon, lat, asset) {
    return {lon: lon, lat: lat, assetGroup: [asset]};
  };

  var closeAsset = function() {
    deselectAsset(selectedAsset);
  };

  var hideAssets = function() {
    assetDirectionLayer.removeAllFeatures();
    assetLayer.setVisibility(false);
  };

  var destroyAsset = function(backendAsset) {
    var uiAsset = assetsModel.getAsset(backendAsset.id);
    if(uiAsset) {
      removeAssetFromMap(uiAsset);
      assetsModel.destroyAsset(backendAsset.id);
    }
  };

  var deselectAsset = function(asset) {
    if (asset) {
      unregisterMouseDownHandler(asset);
      asset.massTransitStop.deselect();
      selectedAsset = null;
    }
  };

  var handleAssetFetched = function(backendAsset) {
    deselectAsset(selectedAsset);
    selectedAsset = assetsModel.getAsset(backendAsset.id);
    registerMouseDownHandler(selectedAsset);
    selectedAsset.massTransitStop.select();
  };

  var moveSelectedAsset = function(pxPosition) {
    if (selectedAsset.massTransitStop.getMarker()) {
      var busStopCenter = new OpenLayers.Pixel(pxPosition.x, pxPosition.y);
      var lonlat = map.getLonLatFromPixel(busStopCenter);
      var nearestLine = geometrycalculator.findNearestLine(roadCollection.getAll(), lonlat.lon, lonlat.lat);
      eventbus.trigger('asset:moving', nearestLine);
      var angle = geometrycalculator.getLineDirectionDegAngle(nearestLine);
      selectedAsset.data.bearing = angle;
      selectedAsset.data.roadDirection = angle;
      selectedAsset.massTransitStop.getDirectionArrow().style.rotation = validitydirections.calculateRotation(angle, selectedAsset.data.validityDirection);
      var position = geometrycalculator.nearestPointOnLine(
        nearestLine,
        { x: lonlat.lon, y: lonlat.lat});
      lonlat.lon = position.x;
      lonlat.lat = position.y;
      selectedAsset.roadLinkId = nearestLine.roadLinkId;
      selectedAsset.data.lon = lonlat.lon;
      selectedAsset.data.lat = lonlat.lat;
      moveMarker(lonlat);
      selectedAssetModel.move({
        lon: lonlat.lon,
        lat: lonlat.lat,
        bearing: angle,
        roadLinkId: nearestLine.roadLinkId
      });
   }
  };

  var moveMarker = function(lonlat) {
    selectedAsset.massTransitStop.moveTo(lonlat);
    assetLayer.redraw();
  };

  var toolSelectionChange = function(action) {
    selectedControl = action;
  };

  var createNewUIAssets = function(backendAssetGroups) {
    return _.map(backendAssetGroups, function(group) {
      var centroidLonLat = geometrycalculator.getCentroid(group);
      return _.map(group, function(backendAsset) {
        return createAsset(convertBackendAssetToUIAsset(backendAsset, centroidLonLat, group));
      });
    });
  };

  var addNewGroupsToModel = function(uiAssetGroups) {
    _.each(uiAssetGroups, assetsModel.insertAssetsFromGroup);
  };

  var renderNewGroups = function(uiAssetGroups) {
    _.each(uiAssetGroups, function(uiAssetGroup) {
      _.each(uiAssetGroup, addAssetToLayersAndSetVisibility);
    });
  };

  var handleNewAssetsFetched = function(newBackendAssets) {
    var backendAssetGroups = assetGrouping.groupByDistance(newBackendAssets, map.getZoom());
    var uiAssetGroups = createNewUIAssets(backendAssetGroups);
    addNewGroupsToModel(uiAssetGroups);
    renderNewGroups(uiAssetGroups);
  };

  var backendAssetsWithSelectedAsset = function(assets) {
    var transformSelectedAsset = function(asset) {
      if (asset) {
        var transformedAsset = asset;
        transformedAsset.lon = selectedAsset.data.lon;
        transformedAsset.lat = selectedAsset.data.lat;
        transformedAsset.bearing = selectedAsset.data.bearing;
        transformedAsset.validityDirection = selectedAsset.data.validityDirection;
        return [transformedAsset];
      }
      return [];
    };
    var transformedSelectedAsset = transformSelectedAsset(_.find(assets, isSelected));
    return _.reject(assets, isSelected).concat(transformedSelectedAsset);
  };

  var updateAllAssets = function(assets) {
    var assetsWithSelectedAsset = backendAssetsWithSelectedAsset(assets);
    var groupedAssets = assetGrouping.groupByDistance(assetsWithSelectedAsset, map.getZoom());
    renderAssets(groupedAssets);
  };

  function handleAllAssetsUpdated(assets) {
    if (zoomlevels.isInAssetZoomLevel(map.getZoom())) {
      updateAllAssets(assets);
    }
  }

  var handleMouseMoved = function(event) {
    if (applicationModel.isReadOnly() || !selectedAsset || !zoomlevels.isInRoadLinkZoomLevel(map.getZoom())) {
      return;
    }
    if (clickTimestamp && (new Date().getTime() - clickTimestamp) >= applicationModel.assetDragDelay &&
      (clickCoords && approximately(clickCoords[0], event.clientX) && approximately(clickCoords[1], event.clientY)) || assetIsMoving) {
      assetIsMoving = true;
      var xAdjustedForClickOffset = event.xy.x - initialClickOffsetFromMarkerBottomleft.x;
      var yAdjustedForClickOffset = event.xy.y - initialClickOffsetFromMarkerBottomleft.y;
      var pixel = new OpenLayers.Pixel(xAdjustedForClickOffset, yAdjustedForClickOffset);
      moveSelectedAsset(pixel);
    }
  };

  var approximately = function(n, m) {
    var threshold = 10;
    return threshold >= Math.abs(n - m);
  };

  var events = map.events;
  var initialClickOffsetFromMarkerBottomleft = { x: 0, y: 0 };
  events.register('mousemove', map, function(event) { eventbus.trigger('map:mouseMoved', event); }, true);

  var Click = OpenLayers.Class(OpenLayers.Control, {
    defaultHandlerOptions: {
      'single': true,
      'double': false,
      'pixelTolerance': 0,
      'stopSingle': false,
      'stopDouble': false
    },

    initialize: function(options) {
      this.handlerOptions = OpenLayers.Util.extend(
        {}, this.defaultHandlerOptions
      );
      OpenLayers.Control.prototype.initialize.apply(
        this, arguments
      );
      this.handler = new OpenLayers.Handler.Click(
        this, {
          'click': this.onClick
        }, this.handlerOptions
      );
    },

    onClick: function(event) {
      eventbus.trigger('map:clicked', {x: event.xy.x, y: event.xy.y});
    }
  });
  var click = new Click();
  map.addControl(click);

  var handleMapClick = function(coordinates) {
    if (selectedControl === 'Add' && zoomlevels.isInRoadLinkZoomLevel(map.getZoom())) {
      var pixel = new OpenLayers.Pixel(coordinates.x, coordinates.y);
      createNewAsset(map.getLonLatFromPixel(pixel));
    } else {
      if (selectedAssetModel.isDirty()) {
        new Confirm();
      } else {
        selectedAssetModel.close();
        window.location.hash = '';
      }
    }
  };


  $('#mapdiv').on('mouseleave', function() {
    if (assetIsMoving === true) {
      mouseUpHandler(selectedAsset);
    }
  });

  var bindEvents = function() {
    eventListener.listenTo(eventbus, 'validityPeriod:changed', handleValidityPeriodChanged);
    eventListener.listenTo(eventbus, 'tool:changed', toolSelectionChange);
    eventListener.listenTo(eventbus, 'assetPropertyValue:saved', updateAsset);
    eventListener.listenTo(eventbus, 'assetPropertyValue:changed', handleAssetPropertyValueChanged);
    eventListener.listenTo(eventbus, 'asset:saved', handleAssetSaved);
    eventListener.listenTo(eventbus, 'asset:created', handleAssetCreated);
    eventListener.listenTo(eventbus, 'asset:fetched', handleAssetFetched);
    eventListener.listenTo(eventbus, 'asset:created', removeOverlay);
    eventListener.listenTo(eventbus, 'asset:creationCancelled asset:creationFailed', cancelCreate);
    eventListener.listenTo(eventbus, 'asset:updateCancelled asset:updateFailed', cancelUpdate);
    eventListener.listenTo(eventbus, 'asset:closed', closeAsset);
    eventListener.listenTo(eventbus, 'assets:fetched', function(assets) {
      if (zoomlevels.isInAssetZoomLevel(map.getZoom())) {
        var groupedAssets = assetGrouping.groupByDistance(assets, map.getZoom());
        renderAssets(groupedAssets);
      }
    });

    eventListener.listenTo(eventbus, 'assets:all-updated', handleAllAssetsUpdated);
    eventListener.listenTo(eventbus, 'assets:new-fetched', handleNewAssetsFetched);
    eventListener.listenTo(eventbus, 'assetModifications:confirm', function() {
      new Confirm();
    });
    eventListener.listenTo(eventbus, 'assets:outOfZoom', hideAssets);
    eventListener.listenTo(eventbus, 'assetGroup:destroyed', reRenderGroup);
    eventListener.listenTo(eventbus, 'map:mouseMoved', handleMouseMoved);

    eventListener.listenTo(eventbus, 'map:clicked', handleMapClick);
    eventListener.listenTo(eventbus, 'layer:selected', closeAsset);

    click.activate();
  };

  var startListening = function() {
    if (!eventListener.running) {
      eventListener.running = true;
      bindEvents();
    }
  };
  startListening();

  var stopListening = function() {
    click.deactivate();
    eventListener.stopListening(eventbus);
    eventListener.running = false;
  };

  var show = function() {
    selectedControl = 'Select';
    startListening();
    map.addLayer(assetDirectionLayer);
    map.addLayer(assetLayer);
    if (zoomlevels.isInAssetZoomLevel(map.getZoom())) {
      assetsModel.fetchAssets(map.getExtent());
    }
  };

  var hide = function() {
    selectedAssetModel.close();
    if (assetLayer.map && assetDirectionLayer.map) {
      map.removeLayer(assetLayer);
      map.removeLayer(assetDirectionLayer);
    }
    stopListening();
  };

  return {
    show: show,
    hide: hide
  };
};

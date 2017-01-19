(function(root) {
  root.RoadLayer3 = function(map, roadCollection) {
    var vectorLayer;
    var drawRoadLinks = function(roadLinks, zoom) {
      console.log("Draw road links");
    };
    var layerMinContentZoomLevels = {};
    var layerStyleMaps = {};
    var layerStyleMapProviders = {};
    var currentZoom = 0;

    var vectorSource = new ol.source.Vector({
      loader: function(extent, resolution, projection) {
        var zoom = Math.log(1024/resolution) / Math.log(2);
        eventbus.once('roadLinks:fetched', function() {
          var features = _.map(roadCollection.getAll(), function(roadLink) {
            var points = _.map(roadLink.points, function(point) {
              return [point.x, point.y];
            });
            return new ol.Feature({ geometry: new ol.geom.LineString(points) });
          });
          loadFeatures(features);
        });
        roadCollection.fetch(extent.join(','), zoom);
      },
      strategy: ol.loadingstrategy.bbox
    });

    var loadFeatures = function (features) {
      vectorSource.addFeatures(features);
    };

    function stylesUndefined() {
      return _.isUndefined(layerStyleMaps[applicationModel.getSelectedLayer()]) &&
        _.isUndefined(layerStyleMapProviders[applicationModel.getSelectedLayer()]);
    }

    var changeRoadsWidthByZoomLevel = function() {
      if (stylesUndefined()) {
        var widthBase = 2 + (map.getView().getZoom() - minimumContentZoomLevel());
        var roadWidth = widthBase * widthBase;
        if (applicationModel.isRoadTypeShown()) {
          vectorLayer.setStyle({stroke: roadWidth});
        } else {
          vectorLayer.setStyle({stroke: roadWidth});
          vectorLayer.styleMap.styles.default.defaultStyle.strokeWidth = 5;
          vectorLayer.styleMap.styles.select.defaultStyle.strokeWidth = 7;
        }
      }
    };

    var minimumContentZoomLevel = function() {
      if (!_.isUndefined(layerMinContentZoomLevels[applicationModel.getSelectedLayer()])) {
        return layerMinContentZoomLevels[applicationModel.getSelectedLayer()];
      }
      return zoomlevels.minZoomForRoadLinks;
    };

    var handleRoadsVisibility = function() {
      if (_.isObject(vectorLayer)) {
        vectorLayer.setVisible(map.getView().getZoom() >= minimumContentZoomLevel());
      }
    };

    var mapMovedHandler = function(mapState) {
      console.log("map moved");
      console.log("zoom = " + mapState.zoom);
      if (mapState.zoom !== currentZoom) {
        currentZoom = mapState.zoom;
        vectorSource.clear();
      }
      // If zoom changes clear the road list
      // if (mapState.zoom >= minimumContentZoomLevel()) {
      //
      //   vectorLayer.setVisible(true);
      //   changeRoadsWidthByZoomLevel();
      // } else {
      //   vectorLayer.clear();
      //   roadCollection.reset();
      // }
      handleRoadsVisibility();
    };


    vectorLayer = new ol.layer.Vector({
      source: vectorSource
    });
    vectorLayer.setVisible(true);
    map.addLayer(vectorLayer);

    eventbus.on('map:moved', mapMovedHandler, this);

    return {
      layer: vectorLayer
    };
  };
})(this);

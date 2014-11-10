var RoadStyles = function() {
  var styleMap = new OpenLayers.StyleMap({
    "select": new OpenLayers.Style({
      strokeWidth: 6,
      strokeOpacity: 1,
      strokeColor: "#5eaedf"
    }),
    "default": new OpenLayers.Style({
      strokeWidth: 5,
      strokeColor: "#a4a4a2",
      strokeOpacity: 0.7
    })
  });

  this.roadStyles = styleMap;
  styleMap.styles.default.rules.push(new OpenLayers.Rule({
    elseFilter: true,
    symbolizer: styleMap.styles.default.defaultStyle
  }));
};

(function(root) {
  root.RoadLayer = function(map, roadCollection) {
    var vectorLayer;
    var selectControl;

    var enableColorsOnRoadLayer = function() {
      var roadLinkTypeStyleLookup = {
        PrivateRoad: { strokeColor: "#0011bb" },
        Street: { strokeColor: "#11bb00" },
        Road: { strokeColor: "#ff0000" }
      };
      vectorLayer.styleMap.addUniqueValueRules("default", "type", roadLinkTypeStyleLookup);
    };

    var disableColorsOnRoadLayer = function() {
      vectorLayer.styleMap.styles.default.rules = [];
    };

    var changeRoadsWidthByZoomLevel = function() {
      var widthBase = 2 + (map.getZoom() - zoomlevels.minZoomForRoadLinks);
      var roadWidth = widthBase * widthBase;
      if (applicationModel.isRoadTypeShown()) {
        vectorLayer.styleMap.styles.default.defaultStyle.strokeWidth = roadWidth;
        vectorLayer.styleMap.styles.select.defaultStyle.strokeWidth = roadWidth;
      } else {
        vectorLayer.styleMap.styles.default.defaultStyle.strokeWidth = 5;
        vectorLayer.styleMap.styles.select.defaultStyle.strokeWidth = 7;
      }
    };

    var toggleRoadType = function() {
      if (applicationModel.isRoadTypeShown()) {
        enableColorsOnRoadLayer();
      } else {
        disableColorsOnRoadLayer();
      }
      changeRoadsWidthByZoomLevel();
      vectorLayer.redraw();
    };

    var handleRoadsVisibility = function() {
      if (_.isObject(vectorLayer)) {
        vectorLayer.setVisibility(zoomlevels.isInRoadLinkZoomLevel(map.getZoom()));
      }
    };

    var fetchRoads = function(bbox) {
      roadCollection.fetch(bbox);
    };

    var mapMovedHandler = function(mapState) {
      if (zoomlevels.isInRoadLinkZoomLevel(mapState.zoom)) {
        fetchRoads(mapState.bbox);
        changeRoadsWidthByZoomLevel();
      } else {
        vectorLayer.removeAllFeatures();
      }
      handleRoadsVisibility();
    };

    var drawRoadLinks = function(roadLinks) {
      vectorLayer.removeAllFeatures();
      var features = _.map(roadLinks, function(roadLink) {
        var points = _.map(roadLink.points, function(point) {
          return new OpenLayers.Geometry.Point(point.x, point.y);
        });
        return new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString(points), roadLink);
      });
      vectorLayer.addFeatures(features);
    };

    eventbus.on('road:active', function(roadLinkId) {
      var nearestFeature = _.find(vectorLayer.features, function(feature) {
        return feature.attributes.roadLinkId == roadLinkId;
      });
      selectControl.unselectAll();
      selectControl.select(nearestFeature);
    }, this);

    eventbus.on('asset:saved asset:updateCancelled asset:updateFailed', function() {
      selectControl.unselectAll();
    }, this);

    eventbus.on('road-type:selected', toggleRoadType, this);

    eventbus.on('map:moved', mapMovedHandler, this);

    eventbus.on('roadLinks:fetched', function(roadLinks) {
      drawRoadLinks(roadLinks);
    }, this);

    eventbus.on('layer:selected', function(layer) {
      if (zoomlevels.isInRoadLinkZoomLevel(map.getZoom())) {
        fetchRoads(map.getExtent());
      }
      toggleRoadType();
    }, this);

    vectorLayer = new OpenLayers.Layer.Vector("road", {
      styleMap: new RoadStyles().roadStyles
    });
    vectorLayer.setVisibility(false);
    selectControl = new OpenLayers.Control.SelectFeature(vectorLayer);
    map.addLayer(vectorLayer);
    toggleRoadType();
  };
})(this);

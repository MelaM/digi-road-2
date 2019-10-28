(function(root) {
    root.AssetLabel = function() {
        var me = this;
        this.MIN_DISTANCE = 0;

        this.getCoordinates = function(points){
            return _.map(points, function(point) {
                return [point.x, point.y];
            });
        };

        this.getCoordinate = function(point){
          return (!_.isUndefined(point.x) ? [point.x, point.y] : [point.lon, point.lat]);
        };

        this.createFeature = function(point){
          if(_.isArray(point))
            return new ol.Feature(new ol.geom.Point(point));
          return new ol.Feature(new ol.geom.Point(me.getCoordinate(point)));
        };

        this.renderFeaturesByPointAssets = function(pointAssets, zoomLevel){
            return me.renderFeatures(pointAssets, zoomLevel, function(asset){
              return me.getCoordinate(asset);
            });
        };

        this.renderFeaturesByLinearAssets = function(linearAssets, zoomLevel){
            return me.renderFeatures(linearAssets, zoomLevel, function(asset){
                var coordinates = me.getCoordinates(me.getPoints(asset));
                var lineString = new ol.geom.LineString(coordinates);
                return GeometryUtils.calculateMidpointOfLineString(lineString);
            });
        };

        this.renderFeatures = function(assets, zoomLevel, getPoint){
          if(!me.isVisibleZoom(zoomLevel))
            return [];

          return _.chain(assets).
          map(function(asset){
            var assetValue = me.getValue(asset);
            if(assetValue !== undefined){
              var style = me.getStyle(asset);
              var feature = me.createFeature(getPoint(asset));
              feature.setProperties(_.omit(asset, 'geometry'));
              feature.setStyle(style);
              return feature;
            }
          }).
          filter(function(feature){ return feature !== undefined; })
            .value();
        };

        this.getGroupedFeatures = function (assets, zoomLevel) {
          var assetGroups = AssetGrouping(me.MIN_DISTANCE).groupByDistance(assets, zoomLevel);
          return _.forEach(assetGroups, function (assetGroup) {
            _.map(assetGroup, function (asset) {
              asset.lon = _.head(assetGroup).lon;
              asset.lat = _.head(assetGroup).lat;
            });
          });
        };

        this.renderGroupedFeatures = function(assets, zoomLevel, getPoint){
          if(!this.isVisibleZoom(zoomLevel))
            return [];
          var groupedAssets = me.getGroupedFeatures(assets, zoomLevel);
          return _.flatten(_.chain(groupedAssets).map(function(assets){
            return _.map(assets, function(asset, index){
              var assetValue = me.getValue(asset);
              if(assetValue !== undefined){
                var styles = me.getStyle(assetValue, index);
                var feature = me.createFeature(getPoint(asset));
                feature.setStyle(styles);
                feature.setProperties(_.omit(asset, 'geometry'));
                return feature;
              }
            });
          }).filter(function(feature){ return !_.isUndefined(feature); }).value());
        };

        this.getMarkerOffset = function(zoomLevel){
            if(me.isVisibleZoom(zoomLevel))
                return [23, 9];
        };

        this.getMarkerAnchor = function(zoomLevel){
            if(me.isVisibleZoom(zoomLevel))
                return [-0.45, 0.15];
        };

        this.isVisibleZoom = function(zoomLevel){
            return zoomLevel >= 12;
        };

        this.getPoints = function(asset){ return asset.points; };

        this.getValue = function(asset){
          return asset.value ? asset.value : undefined;
        };
        
        this.isSuggested = function(asset){
          var suggestionBox =  _.some(asset.value, function(value) {
            var suggestionBox = _.find(value, function(prop) {
              return prop.publicId === 'suggest_box';});
            return (!_.isUndefined(suggestionBox) && suggestionBox.values.length !==0 && !!parseInt(_.head(suggestionBox.values).value));});
          
          return _.has(asset, 'value') && suggestionBox;
        };


        this.getSuggestionStyle = function (position) {
          position = _.isUndefined(position) ? {x:0, y:0} : position;
            return new ol.style.Style({
                image: new ol.style.Icon(({
                    src: 'images/icons/questionMarker.png',
                    anchor : [position.x, 1 + position.y]
                }))
            });
        };
        
        this.defaultStyle = function(value){};
      
        this.getStyle = function(asset){
          if(this.isSuggested(asset))
            return this.getSuggestionStyle();
          else
            return this.defaultStyle(this.getValue(asset));
        };

        this.suggestionStyle = function(suggestionInfo, styles, position) {
            return !_.isUndefined(suggestionInfo) && !!parseInt(suggestionInfo.propertyValue) ?
                styles.concat(me.getSuggestionStyle(position)) : styles;
        };
    };
})(this);

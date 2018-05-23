(function(root) {

    root.LinearAssetLabel = function(){
        AssetLabel.call(this);
        var me = this;

        var backgroundStyle = function (value) {

          var valueLength = value.toString().length;
          var image = 'images/linearLabel_background.png';

          if (!me.isValidValue(value)) {
            image = 'images/warningLabel.png';
          }else if (valueLength > 4 && valueLength < 7) {
            image = 'images/linearLabel_background_large.png';
          }

          return new ol.style.Style({
            image: new ol.style.Icon(({
              src: image
            }))
          });
        };

        var textStyle = function(value) {
          if (!me.isValidValue(value))
            return '';
          return "" + value;
        };

        this.isValidValue = function(value){
          var valueLength = value.toString().length;
          if(value)
            if(valueLength > 6 || value < 0)
              return false;
            return true;
        };

        this.getStyle = function(value){
          return [backgroundStyle(value), new ol.style.Style({
            text : new ol.style.Text({
              text : textStyle(value),
              fill: new ol.style.Fill({
                  color: '#ffffff'
              }),
              font : '12px sans-serif'
            })
          })];
        };

        this.getValue = function(asset){
            return asset.value;
        };

    };

    root.SpeedLimitAssetLabel = function() {
        LinearAssetLabel.call(this);

        this.isValidValue = function(value) {
            return value && value > 0 && value <= 120;
        };
    };


    root.TRSpeedLimitAssetLabel = function () {
      LinearAssetLabel.call(this);
      var me = this;

      var backgroundStyle = function (value) {
        return new ol.style.Style({
          image: new ol.style.Icon(({
            src: getImageConfiguration(value).image,
            scale : getImageConfiguration(value).scale
          }))
        });
      };

      this.getStyle = function(value){
        return [backgroundStyle(value), new ol.style.Style({
          text : new ol.style.Text({
            text : textStyle(value),
            fill: new ol.style.Fill({
              color: '#ffffff'
            }),
            font : '12px sans-serif'
          })
        })];
      };


      this.isValidValue = function(value) {
        return value && value >= 20 && value <= 120;
      };

      var getImageConfiguration = function (value) {

        var imagesConfig = [
          {range : [{min: 60, max: 70}, {min: 120, max: 121}] , image: 'images/speed-limits/blueCircle.svg', scale: 1.6 },
          {range : [{min: 40, max: 50}, {min: 100, max: 120}]  , image: 'images/speed-limits/greenCircle.svg', scale: 1.6  },
          {range : [{min: 20, max: 30}, {min: 70, max: 80}] , image: 'images/speed-limits/lightBlueCircle.svg', scale: 1.6  },
          {range : [{min: 50, max: 60}, {min: 80, max: 90}] , image: 'images/speed-limits/redCircle.svg', scale: 1.6 },
          {range : [{min: 30, max: 40}, {min: 90, max: 100}], image: 'images/speed-limits/pinkCircle.svg' , scale: 1.6 }
        ];


        var config = imagesConfig.find ( function(config) {
          return _.some(config.range, function(range) { return range.min <= value && range.max > value; });
        });

        if(config)
          return config;

        return {image: 'images/warningLabel.png', scale: 1};
      };

      var textStyle = function(value) {
        if (!me.isValidValue(value))
          return '';
        return '' + value;
      };

    };


  root.LinearAssetLabelMultiValues = function(){

        AssetLabel.call(this);

        var me = this;
        var IMAGE_HEIGHT = 27;
        var IMAGE_ADJUSTMENT = 15;

        this.getStyle = function(value){
          return createMultiStyles(value);
        };

        var createMultiStyles = function(values){
          var i = 0;
          var splitValues = values.replace(/[ \t\f\v]/g,'').split(/[\n,]+/);
          var styles = [];
          _.forEach(splitValues, function(value){
            i++;
            styles.push(backgroundStyle(value, i), textStyle(value, i));
          });
          return styles;
        };

        var backgroundStyle = function(value, i){
          var image = 'images/linearLabel_background.png';
          if(!correctValues(value))
            image = 'images/warningLabel.png';

          return new ol.style.Style({
            image: new ol.style.Icon(({
              anchor: [IMAGE_ADJUSTMENT+2, (i * IMAGE_HEIGHT) - IMAGE_ADJUSTMENT],
              anchorXUnits: 'pixels',
              anchorYUnits: 'pixels',
              src: image
            }))
          });
        };

        var textStyle = function(value, i) {

          return new ol.style.Style({
            text: new ol.style.Text(({
              text: getTextValue(value),
              offsetX: 0,
              offsetY: (-i*IMAGE_HEIGHT)+IMAGE_HEIGHT,
              textAlign: 'center',
              fill: new ol.style.Fill({
                color: '#ffffff'
              }),
              font : '12px sans-serif'
            }))
          });
        };

        var getTextValue = function(value) {
          if(!correctValues(value))
            return '';
          return '' + value;
        };

        var correctValues = function(value){
          var valueLength = value.toString().length;
          if(value){
            return value.match(/^[0-9|Ee][0-9|Bb]{0,2}/) && valueLength < 4;
          }
          return true;
        };

        this.getValue = function(asset){
            return asset.value;
        };

    };

    root.MassLimitationsLabel = function () {

      AssetLabel.call(this);
      var me = this;

      var backgroundStyle = function (value, counter) {
        return new ol.style.Style({
          image: new ol.style.Icon(({
            src: getImage(value),
            anchor : [0.5, 1 + counter]
          }))
        });
      };

      var textStyle = function (value) {
        if (_.isUndefined(value))
          return '';
        // conversion Kg -> t
        return ''.concat(value/1000, 't');
      };

      this.getStyle = function (asset, counter) {
        return [backgroundStyle(getTypeId(asset), counter),
          new ol.style.Style({
            text: new ol.style.Text({
              text: textStyle(me.getValue(asset)),
              fill: new ol.style.Fill({
                color: '#000000'
              }),
              font: '14px sans-serif',
              offsetY: getTextOffset(asset, counter)
            })
        })];
      };

      var getImage = function (typeId) {
        var images = {
          30: 'images/mass-limitations/totalWeightLimit.png'   ,
          40: 'images/mass-limitations/trailerTruckWeightLimit.png',
          50: 'images/mass-limitations/axleWeightLimit.png',
          60: 'images/mass-limitations/bogieWeightLimit.png'
        };
        return images[typeId];
      };


      var getTextOffset = function (asset, counter) {
        var offsets = { 30: -17 - (counter * 35), 40: -12 - (counter * 35), 50: -20 - (counter * 35), 60: -20 - (counter * 35)};
        return offsets[getTypeId(asset)];
      };

      var getValues = function (asset) {
        return asset.values;
      };

      this.getValue = function (asset) {
        return asset.value;
      };

      var getTypeId = function (asset) {
        return asset.typeId;
      };

      this.renderFeatures = function (assets, zoomLevel, getPoint) {
        if (!me.isVisibleZoom(zoomLevel))
          return [];


        return [].concat.apply([], _.chain(assets).map(function (asset) {
          var values = getValues(asset);
          return _.map(values, function (assetValues, index) {
            var style = me.getStyle(assetValues, index);
            var feature = me.createFeature(getPoint(asset));
            feature.setStyle(style);
            return feature;
          });
        }).filter(function (feature) {
          return !_.isUndefined(feature);
        }).value());
      };
    };

    root.RoadDamagedByThawLabel = function () {
        AssetLabel.call(this);
        var me = this;
        var IMAGE_SIGN_HEIGHT = 33;
        var IMAGE_SIGN_ADJUSTMENT = 15;
        var IMAGE_LABEL_HEIGHT = 58;
        var IMAGE_LABEL_ADJUSTMENT = 43;

        this.getStyle = function (values) {
            var value = values.properties[0].values[0] ? values.properties[0].values[0].value :'' ;
            return createMultiStyles(value);
        };

        var createMultiStyles = function (value) {
            var stylePositions = [1,2];
            return _.flatten(_.map(stylePositions, function(position){
                return [backgroundStyle(value, position), textStyle(value, position)];
            }));
        };

        var backgroundStyle = function(value, pos){
            var image = getImage(pos);
            var anchor = pos > 1 ? [IMAGE_SIGN_ADJUSTMENT + 2, (pos * IMAGE_SIGN_HEIGHT) - IMAGE_SIGN_ADJUSTMENT] : [IMAGE_LABEL_ADJUSTMENT, IMAGE_LABEL_HEIGHT - IMAGE_LABEL_ADJUSTMENT];

            if (!isValidValue(value) && (pos > 1))
                image = 'images/warningLabel_red_yellow.png';

            return new ol.style.Style({
                image: new ol.style.Icon(({
                    anchor: anchor,
                    anchorXUnits: 'pixels',
                    anchorYUnits: 'pixels',
                    src: image
                }))
            });
        };

        var textStyle = function(value, pos) {
            var textValue;
            if (pos === 1) {
                textValue = 'Kelirikko';
            } else {
                textValue = getTextValue(value);
            }

            return new ol.style.Style({
                text: new ol.style.Text(({
                    text: textValue,
                    offsetX: 0,
                    offsetY:(-pos*IMAGE_SIGN_HEIGHT)+IMAGE_SIGN_HEIGHT,
                    textAlign: 'center',
                    fill: new ol.style.Fill({
                        color: '#000000'
                    }),
                    font: 'bold 14px sans-serif'
                }))
            });
        };

        var getTextValue = function (value) {
            if (_.isUndefined(value) || !isValidValue(value))
                return '';

            return ''.concat(value/1000, 't');
        };

        var isValidValue = function (value) {
            var valueLength = value.toString().length;
            if (valueLength === 0 || value < 0)
                return false;
            return true;
        };

        this.getValue = function (asset) {
            return asset.value;
        };

        var getImage = function (position) {
            var images = {
                1: 'images/linearLabel_largeText_yellow_red.png',
                2: 'images/mass-limitations/totalWeightLimit.png'
            };
            return images[position];
        };
    };


})(this);

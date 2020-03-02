(function(root) {
  root.LocationSearch = function(backend, applicationModel) {
    var selectedLayer;

    this.privateRoadAssociationNames = function() {
      return backend.getPrivateRoadAssociationNames().then(function(result){
        eventbus.trigger('associationNames:fetched', result);
      });
    };

    /**
     * Search by street address
     *
     * @param street
     * @returns {*}
     */
    var geocode = function(street) {
      return backend.getGeocode(street.address).then(function(result) {
        var resultLength = _.get(result, 'length');
        var vkmResultToCoordinates = function(r) {
          return { title: r.tienimiFi +" "+ r.katunumero + ", "+ r.kuntaNimi, lon: r.x, lat: r.y ,resultType:"street" };
        };
        if (resultLength > 0) {
          return _.map(result, vkmResultToCoordinates);
        } else {
          return $.Deferred().reject('Tuntematon katuosoite');
        }
      });
    };

    /**
     * Combined numerical value search (asset id and road number, which is part of road address)
     *
     * @param input
     * @returns {*}
     */
    var idOrRoadNumber = function(input) {
      if (selectedLayer === 'massTransitStop') {
        return roadNumberAndNationalIdSearch(input);
      } else if (selectedLayer === 'linkProperty') {
        return roadNumberAndRoadLinkSearch(input);
      } else if (selectedLayer === 'speedLimit') {
        return roadNumberAndSpeedLimitSearch(input.text);
      } else {
        return getCoordinatesFromRoadAddress({roadNumber: input.text});
      }
    };

    /**
     * Speed limit id search, combined with road number search
     *
     * @param input
     * @returns {*}
     */
    var roadNumberAndSpeedLimitSearch = function(input){
      var roadNumberSearch = backend.getCoordinatesFromRoadAddress(input);
      var speedlimitSearch= backend.getSpeedLimitsLinkIDFromSegmentID(input);
      return $.when(
        speedlimitSearch, roadNumberSearch).then(function(speedlimitdata,roadData) {
        var returnObject = roadLocationAPIResultParser(roadData);
        if (_.get(speedlimitdata[0], 'success')) {
          var linkid = _.get(speedlimitdata[0], 'linkId');
          var y = _.get(speedlimitdata[0], 'latitude');
          var x= _.get(speedlimitdata[0], 'longitude');
          var title = input + " (nopeusrajoituksen ID)";
            returnObject.push({title: title, lon: x, lat: y, linkid:linkid, resultType:"SpeedLimit"});
        }
        if (returnObject.length===0){
          return $.Deferred().reject('Haulla ei löytynyt tuloksia');
        }
        return returnObject;
        });
    };

    /**
     * Link id search, combined with road number search
     *
     * @param input
     * @returns {*}
     */
    var roadNumberAndRoadLinkSearch= function(input) {
      var roadLinkSearch = backend.getRoadLinkToPromise(input.text);
      var roadNumberSearch = backend.getCoordinatesFromRoadAddress(input.text);
      return $.when(roadLinkSearch, roadNumberSearch).then(function(linkdata,roadData) {
        var returnObject = roadLocationAPIResultParser(roadData);
        if (_.get(linkdata[0], 'success')) {
          var x = _.get(linkdata[0], 'middlePoint.x');
          var y = _.get(linkdata[0], 'middlePoint.y');
          var title = input.text + " (linkin ID)";
          returnObject.push({title: title, lon: x, lat: y, resultType: "Link-id"});
        }
          if (returnObject.length === 0){
          return $.Deferred().reject('Haulla ei löytynyt tuloksia');
          }
        return returnObject;
      });
    };

    /**
     * Mass transit stop national id search, combined with road number search
     *
     * @param input
     * @returns {*}
     */
    var roadNumberAndNationalIdSearch = function(input) {
      return $.when(backend.getMassTransitStopByNationalIdForSearch(input.text), backend.getCoordinatesFromRoadAddress(input.text)).then(function(result,roadData) {
        var returnObject = roadLocationAPIResultParser(roadData);
         if (_.get(result[0], 'success')) {
          var lon = _.get(result[0], 'lon');
          var lat = _.get(result[0], 'lat');
          var title = input.text + ' (valtakunnallinen ID)';
          returnObject.push({title: title, lon: lon, lat: lat, nationalId: input.text,resultType:"Mtstop"});
         }
        if (returnObject.length === 0){
          return $.Deferred().reject('Haulla ei löytynyt tuloksia');
        }
            return returnObject;
      });
    };

    /**
     * Road address search
     *
     * @param roadData
     * @returns {*}
     */
    function roadLocationAPIResultParser(roadData){
      var constructTitle = function(road) {
        var titleParts = [_.get(road, 'tie'), _.get(road, 'osa'), _.get(road, 'etaisyys'), _.get(road, 'ajorata')];
        return _.some(titleParts, _.isUndefined) ? '' : titleParts.join(' ');
      };
      var auxRoadData = _.head(roadData);
      var lon = _.get(auxRoadData, 'x');
      var lat = _.get(auxRoadData, 'y');
      var title = constructTitle(auxRoadData);
      if (lon && lat) {
        return  [{title: title, lon: lon, lat: lat, resultType:"road"}];
      } else {
        return [];
      }
    }

    /**
     * Search by mass transit stop Livi-id
     *
     * @param input
     * @returns {*}
     */
    var  massTransitStopLiviIdSearch = function(input) {
      return $.when(backend.getMassTransitStopByLiviIdForSearch(input.text), backend.getCoordinatesFromRoadAddress(input.text)).then(function(result,roadData) {
        var returnObject = roadLocationAPIResultParser(roadData);
        if (_.get(result[0], 'success')) {
          var lon = _.get(result[0], 'lon');
          var lat = _.get(result[0], 'lat');
          var nationalid=_.get(result[0], 'nationalId');
          var title = input.text + ' (pysäkin Livi-tunniste)';
          returnObject.push({title: title, lon: lon, lat: lat, nationalId: nationalid, resultType:"Mtstop"});
        }
          if (returnObject.length === 0){
            return $.Deferred().reject('Haulla ei löytynyt tuloksia');
          }
          return returnObject;
      });
    };

    /**
     * Search by mass transit stop passenger id
     *
     * @param input
     * @returns {*}
     */
    var  massTransitStopPassengerIdSearch = function(input) {
      return $.when(backend.getMassTransitStopByPassengerIdForSearch(input.text)).then(function(result) {
        var toCoordinates = function (r) {
          var title = input.text + ', ' + r.municipalityName;
          return {title: title, lon: r.lon, lat: r.lat, nationalId: r.nationalId, resultType: "Mtstop"};
        };

        if (result.length > 0)
          return _.map(result, toCoordinates);
        return $.Deferred().reject('Haulla ei löytynyt tuloksia');
      });
    };

    /**
     * Get road address coordinates
     *
     * @param road
     * @returns {*}
     */
    var getCoordinatesFromRoadAddress = function(road) {
      return backend.getCoordinatesFromRoadAddress(road.roadNumber, road.section, road.distance, road.lane)
        .then(function(resultfromapi) {
          var searchResult = roadLocationAPIResultParser(resultfromapi);
          if (searchResult.length === 0) {
            return $.Deferred().reject('Tuntematon tieosoite');
          } else {
            return searchResult;
          }
        });
    };

    /**
     * Search private road association names.
     *
     * @param associationRoadName
     */
    var getAssociationRoadNamesByName = function(associationRoad) {
      var associationRoadName = associationRoad.name.toUpperCase().replace(/\s{2,}/g,' ').trim();
      return backend.getPrivateRoadAssociationNamesBySearch(associationRoadName)
        .then(function(resultFromAPI) {
          if (resultFromAPI.length > 0)
            return _.map(resultFromAPI, function(value) {
              var title = value.name + ", " + value.municipality + ", " + value.roadName;
              return { title: title, linkId: value.linkId, resultType: "association" };
            });
          else
            return $.Deferred().reject('Hakusanalla ei löydetty sopivaa tiekuntanimeä.');
        });
    };

    /**
     * Search by coordinates
     *
     * @param coordinates
     * @returns {*|String}
     */
    var resultFromCoordinates = function(coordinates) {
      var result = _.assign({}, coordinates, { title: coordinates.lat + ',' + coordinates.lon, resultType:"coordinates" });
      return $.Deferred().resolve([result]);
    };

    /**
     * Main search method
     *
     * @param searchString
     * @returns {*}
     */
    this.search = function(searchString) {
      function addDistance(item) {
        var currentLocation = applicationModel.getCurrentLocation();

        var distance = GeometryUtils.distanceOfPoints({
          x: currentLocation.lon,
          y: currentLocation.lat
        }, {
          x: item.lon,
          y: item.lat
        });
        return _.assign(item, {
          distance: distance
        });
      }

      selectedLayer = applicationModel.getSelectedLayer();
      var input = LocationInputParser.parse(searchString, selectedLayer);
      var resultByInputType = {
        coordinate: resultFromCoordinates,
        street: geocode,
        road: getCoordinatesFromRoadAddress,
        idOrRoadNumber: idOrRoadNumber,
        liviId: massTransitStopLiviIdSearch,
        passengerId:  massTransitStopPassengerIdSearch,
        roadAssociationName: getAssociationRoadNamesByName,
        invalid: function() { return $.Deferred().reject('Syötteestä ei voitu päätellä koordinaatteja, katuosoitetta tai tieosoitetta'); }
      };

      var results = resultByInputType[input.type](input);
      return results.then(function(result) {
        return _.map(result, addDistance);
      });
    };
  };
})(this);

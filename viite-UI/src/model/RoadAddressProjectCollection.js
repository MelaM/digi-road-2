(function(root) {
  root.RoadAddressProjectCollection = function(backend) {
    var roadAddressProjects = [];
    var roadAddressProjectLinks = [];

    this.getProjects = function(){
      return backend.getRoadAddressProjects(function(projects){
        roadAddressProjects = projects;
      });
    };

    this.getProjectsWithLinksById = function(projectId){
      return backend.getProjectsWithLinksById(projectId, function(projects){
        roadAddressProjects = projects.project;
        roadAddressProjectLinks = projects.projectLinks;
      });
    };

    this.clearRoadAddressProjects = function(){
      roadAddressProjects = [];
    };


    this.createProject = function(data, currentProject){
      var dataJson = {id: 0, status:1 ,name : data[0].value, startDate: data[1].value , additionalInfo :  data[2].value, roadNumber : data[3].value === '' ? 0 : parseInt(data[3].value), startPart: data[4].value === '' ? 0 : parseInt(data[4].value), endPart : data[5].value === '' ? 0 : parseInt(data[5].value) };
      backend.createRoadAddressProject(dataJson, function(result) {
        eventbus.trigger('roadAddress:projectSaved', result);
      }, function() {
        eventbus.trigger('roadAddress:projectFailed');
      });
    };

  };
})(this);

<script>

	/* Draw the map */
	async function initMap(){ 

		// Create map initial info
		const mapCenter = { lat: 37.977707907538836, lng: 23.726837568280057 }
		const mapZoom = 6

		// Setup map element
		const mapElement = document.getElementById('project-map')
		const map = new google.maps.Map( mapElement, { zoom: mapZoom, center: mapCenter })
		// Call marker placing
		drawMarkers(map)
	}
	
	
	/* Draw markers on map */
	var keep_poly = []
	var keep_markers = []
	var draw_poly = false;
	var draw2_poly = false;
	var line,line2;
	var iot_lat,iot_lng;
	async function drawMarkers(map){
		
		// Get info from DB
		var devices = await getDevicesInfo()
		devices = devices.devices
		var events = await getEventsInfo()
		events = events.events
		
		
		// Iterate through to create markers
		var tempMarkerPosition, tempMarker, danger_level, iconSrc, infowindow, infos, device_id, dev_id,android_lat,android_lng;
		
		devices.forEach(device => {
			tempMarkerPosition = { lat: parseFloat(device['latitude']), lng: parseFloat(device['longitude']) }
			device_id = device['device_id']

			// Android
			if(device.device_type == "android"){
				iconSrc = "smartphone.png"
				
				tempMarker = new google.maps.Marker({
					position: tempMarkerPosition,
					map: map,
					icon: iconSrc
				})
				infowindow = new google.maps.InfoWindow({
					content:" "
				});

				google.maps.event.addListener(tempMarker, 'click', function() {
					infowindow.setContent("<h3> Android Device </h3> Latitude: " + device['latitude'] + " & Longtitude: " + device['longitude'] + "<br>Device ID: " + device['device_id']);
					infowindow.open(map,this);
				});
				android_lat=tempMarkerPosition.lat;
				android_lng=tempMarkerPosition.lng;
			}
			else{

				danger_level = parseInt(device['danger_level'])

				if(keep_markers.includes(tempMarkerPosition.lat)){

					// tempMarker.setMap(null)
					tempMarker = null

					// keep_markers.pop(tempMarkerPosition.lat)
				}
				else{
					keep_markers.push(tempMarkerPosition.lat)

				}

				console.log(keep_markers)
				
				if(danger_level == 2){
					iconSrc = "danger_red_30.png"
				} else if(danger_level == 1){
					iconSrc = "danger_yellow_30.png"
				} else {
					iconSrc = "danger_black_30.png"
				}

				tempMarker = new google.maps.Marker({
					position: tempMarkerPosition,
					map: map,
					icon: iconSrc
				})
				
				infowindow = new google.maps.InfoWindow({
					content:" "
				});
			
			
				
				// infowindow content
				let content = "<h3> IoT Device </h3> Latitude: " + tempMarkerPosition.lat + " & Longtitude: " + tempMarkerPosition.lng
				let fin = false
	
				events.forEach(event => {
					if(fin == false){
						dev_id = event['device_id']
						
						if(dev_id == device.device_id){
							infos = { type: event['type'], value: parseFloat(event['value']), sensor_id: event['sensor_id'] }
							
							// print sensor type and value
							content = content + " <br>"+ infos.type + ": " + infos.value 
							
							if(infos.sensor_id == 0){
								fin = true
							}
						}

						google.maps.event.addListener(tempMarker, 'click', function() {
							infowindow.setContent(content);
							infowindow.open(map,this);
						});

					}
				})

			
				
				if(danger_level == 2){
	

					if(!(keep_poly.includes(tempMarkerPosition.lat))){
						keep_poly.push(tempMarkerPosition.lat)
						// console.log(keep_poly)

						if(!draw_poly){
							line = new google.maps.Polyline({
								path: [
									new google.maps.LatLng(android_lat,android_lng),
									new google.maps.LatLng(tempMarkerPosition.lat, tempMarkerPosition.lng)
								],
								strokeColor: "#50C878",
								strokeOpacity: 1.0,
								strokeWeight: 5,
								map: map
							});
							draw_poly = true
						}
						else{
							line.getPath().removeAt();

							if(draw2_poly){
								line2.getPath().removeAt();
							}

							line = new google.maps.Polyline({
								path: [
									new google.maps.LatLng(iot_lat,iot_lng),
									new google.maps.LatLng((tempMarkerPosition.lat), (tempMarkerPosition.lng))
								],
								strokeColor: "#50C878",
								strokeOpacity: 1.0,
								strokeWeight: 5,
								map: map
							});
		
							line2 = new google.maps.Polyline({
								path: [
									new google.maps.LatLng(android_lat,android_lng),
									new google.maps.LatLng((tempMarkerPosition.lat+iot_lat)/2, (tempMarkerPosition.lng+iot_lng)/2)
								],
								strokeColor: "#50C878",
								strokeOpacity: 1.0,
								strokeWeight: 5,
								map: map
		
							});

							draw2_poly = true
						}
						
						// Calculate the distance in kilometers between markers
						var kms=(getDistanceFromLatLonInKm(android_lat,android_lng,tempMarkerPosition.lat,tempMarkerPosition.lng));


					}

					iot_lat=tempMarkerPosition.lat;
					iot_lng=tempMarkerPosition.lng;
					
					
				}
				// else{
				// 	if(draw2_poly){
				// 		var lng
				// 		var arr = line.getPath()
				// 		var lat = arr.getAt(0).lat()

				// 		if(tempMarkerPosition.lat == lat){
				// 			lat = arr.getAt(1).lat()
				// 			lng = arr.getAt(1).lng()
				// 		}
				// 		else{
				// 			lng = arr.getAt(0).lng()

				// 		}

				// 		line.getPath().removeAt();
				// 		line2.getPath().removeAt();
						
				// 		line = new google.maps.Polyline({
				// 			path: [
				// 				new google.maps.LatLng(android_lat,android_lng),
				// 				new google.maps.LatLng(lat, lng)
				// 			],
				// 			strokeColor: "#50C878",
				// 			strokeOpacity: 1.0,
				// 			strokeWeight: 5,
				// 			map: map
				// 		});

				// 		draw2_poly = false
				// 		iot_lat=tempMarkerPosition.lat;
				// 		iot_lng=tempMarkerPosition.lng;
				// 	}
				// }
		
			}   

			tempMarker.setMap(map)
		})


		
		setTimeout(drawMarkers, 2000, map)

		// drawArea(devices, map)
		drawCirle(devices, map);

	}

	function getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2) {
		var R = 6371; // Radius of the earth in km
		var dLat = deg2rad(lat2-lat1);  // deg2rad below
		var dLon = deg2rad(lon2-lon1); 
		var a = 
			Math.sin(dLat/2) * Math.sin(dLat/2) +
			Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
			Math.sin(dLon/2) * Math.sin(dLon/2)
			; 
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		var d = R * c; // Distance in km
		return d;
	}

	function deg2rad(deg) {
		return deg * (Math.PI/180)
	}


	/* Draw area on map */
	async function drawArea(devices, map){
		
		// Settings for area
		var strokeColor = "#FF0000"
		var strokeOpacity = 0.8
		var strokeWeight = 0
		var fillColor = "#FF0000"
		var fillOpacity = 0.35

		// Setup Coordinates for area
		var polygonCoordinates = []

		var coordinates;
		devices.forEach(device => {
			coordinates = { lat: parseFloat(device['latitude']), lng: parseFloat(device['longitude']) }
			polygonCoordinates.push(coordinates)
		})
		var bounds = {
			north: Math.max(polygonCoordinates[0]['lat'], polygonCoordinates[1]['lat']),
			south: Math.min(polygonCoordinates[0]['lat'], polygonCoordinates[1]['lat']),
			east: Math.max(polygonCoordinates[0]['lng'], polygonCoordinates[1]['lng']),
			west: Math.min(polygonCoordinates[0]['lng'], polygonCoordinates[1]['lng']),
    	}
	
		// Setting for area polygon
		const mapArea = new google.maps.Rectangle({
			bounds: bounds,
			map: map,
			strokeColor: strokeColor,
			strokeOpacity: strokeOpacity,
			strokeWeight: strokeWeight,
			fillColor: fillColor,
			fillOpacity: fillOpacity,
		})

		// Set the area onto the map
		mapArea.setMap(map)

	}


	var keep_circles = []
	async function drawCirle(devices, map){
		var deviceCoordinates = []

		var coordinates;
		devices.forEach(device => {
			coordinates = { lat: parseFloat(device['latitude']), lng: parseFloat(device['longitude']), type: device['device_type'] }

			// Make sure it is an IoT device
			if(coordinates.type == "iot"){
				deviceCoordinates.push(coordinates)
			}
		})

		let green = "#00FF7F";
		let red = "#FF6347";
	
		// Setting for the circles
		for(let i=0; i<deviceCoordinates.length; i++){

			var center = {
				lat : deviceCoordinates[i]['lat'], 
				lng : deviceCoordinates[i]['lng'],
			}

			if(!(keep_circles.includes(center.lat))){
				keep_circles.push(center.lat)
				// console.log(keep_circles)
				

				// !! check if we want red or green 

				var iotCircle = new google.maps.Circle({
					strokeColor: "#32CD32",
					strokeOpacity: 10,
					strokeWeight: 0,
					fillColor: green,
					fillOpacity: 0.35,
					map: map,
					center: center,
					radius: 10000,
				})
				// Set the area onto the map
				iotCircle.setMap(map)
			}
		}


	}



	/* Getting devices information */
	async function getDevicesInfo(){

		const url = 'http://localhost:4000/devices'

		const response = await fetch(url, {
			method: 'GET',
			headers: { 'Content-Type': 'application/json' },
		})

		return await response.json()
	}

	async function getEventsInfo(){

		const url = 'http://localhost:4000/events'

		const response = await fetch(url, {
			method: 'GET',
			headers: { 'Content-Type': 'application/json' },
		})

		return await response.json()
	}

</script>

<svelte:head>
  <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyAOI56x1KU5h5A8CV9UuIs1lEfjnZQCIa8" on:load={initMap}></script>
</svelte:head>

<main>
	<div id="project-map"></div>
</main>

<style>
	main {
		text-align: center;
		padding: 1em;
		max-width: 240px;
		margin: 0 auto;
	}

	#project-map {
		height: 650px;
		 width: 1865px;
	}

	@media (min-width: 640px) {
		main {
			max-width: none;
		}
	}
</style>
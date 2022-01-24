<script>

	/* Draw the map */
	async function initMap(){ 

		// Create map initial info
		const mapCenter = { lat: 37.977707907538836, lng: 23.726837568280057 }
		const mapZoom = 4

		// Setup map element
		const mapElement = document.getElementById('project-map')
		const map = new google.maps.Map( mapElement, { zoom: mapZoom, center: mapCenter })

		// Call marker placing
		drawMarkers(map)
	}


	/* Draw markers on map */
	async function drawMarkers(map){
		
		// Get info from DB
		var devices = await getDevicesInfo()
		devices = devices.devices
		
		// Iterate through to create markers
		var tempMarkerPosition, tempMarker;
		devices.forEach(device => {
			tempMarkerPosition = { lat: parseFloat(device['latitude']), lng: parseFloat(device['longitude']) }
			tempMarker = new google.maps.Marker({
				position: tempMarkerPosition,
				map: map
			})
			tempMarker.setMap(map)
		})

		drawArea(devices, map)
		setTimeout(drawMarkers, 2000, map)

	}


	/* Draw area on map */
	async function drawArea(devices, map){
		
		// Settings for area
		var strokeColor = "#FF0000"
		var strokeOpacity = 0.8
		var strokeWeight = 2
		var fillColor = "#FF0000"
		var fillOpacity = 0.35

		// Setup Coordinates for area
		const triangleCoords = []

		var coordinates;
		devices.forEach(device => {
			coordinates = { lat: parseFloat(device['latitude']), lng: parseFloat(device['longitude']) }
			triangleCoords.push(coordinates)
		})

		// Construct area polygon
		const mapArea = new google.maps.Polygon({
			paths: triangleCoords,
			strokeColor: strokeColor,
			strokeOpacity: strokeOpacity,
			strokeWeight: strokeWeight,
			fillColor: fillColor,
			fillOpacity: fillOpacity,
		})

		// Set the area onto the map
		mapArea.setMap(map)
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
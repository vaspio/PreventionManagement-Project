<script>
	export let name;

	/* Draw the map */
	async function initMap(){ 

		// Create map initial info
		const mapCenter = { lat: 37.977707907538836, lng: 23.726837568280057 }
		const mapZoom = 4

		// Setup map element
		const mapElement = document.getElementById('project-map')
		const map = new google.maps.Map( mapElement, { zoom: mapZoom, center: mapCenter })

		// Self-refreshing drawings
		drawMarkers(map)
	}


	/* Getting latest locations of androids and iot device */
	async function getLatestLocations(){

		const url = 'http://localhost:4000/events'

		const response = await fetch(url, {
			method: 'GET',
			headers: { 'Content-Type': 'application/json' },
		})

		return await response.json()
	}


	/* Draw markers on map */
	async function drawMarkers(map){
		
		// Get locations from DB
		var locations = await getLatestLocations()
		locations = locations.events
		
		// Create markers and add lat/lng
		var tempMarkerPosition = { lat: parseFloat(await locations[0]['latitude']), lng: parseFloat(await locations[0]['longitude']) }
		var marker1 = new google.maps.Marker({
			position: tempMarkerPosition,
			map: map
		})
		tempMarkerPosition = { lat: parseFloat(await locations[1]['latitude']), lng: parseFloat(await locations[1]['longitude']) }
		var marker2 = new google.maps.Marker({
			position: tempMarkerPosition,
			map: map
		})
		tempMarkerPosition = { lat: parseFloat(await locations[2]['latitude']), lng: parseFloat(await locations[2]['longitude']) }
		var marker3 = new google.maps.Marker({
			position: tempMarkerPosition,
			map: map
		})

		// Add markers to the map
		marker1.setMap(map)
		marker2.setMap(map)
		marker3.setMap(map)

		drawArea(locations, map)
		setTimeout(drawMarkers, 3000, map)
	}


	/* Draw area on map */
	async function drawArea(locations, map){
		
		// Settings for area
		var strokeColor = "#FF0000"
		var strokeOpacity = 0.8
		var strokeWeight = 2
		var fillColor = "#FF0000"
		var fillOpacity = 0.35

		// Setup Coordinations for area
		const triangleCoords = [
			{ lat: parseFloat(await locations[0]['latitude']), lng: parseFloat(await locations[0]['longitude']) },
			{ lat: parseFloat(await locations[1]['latitude']), lng: parseFloat(await locations[1]['longitude']) },
			{ lat: parseFloat(await locations[2]['latitude']), lng: parseFloat(await locations[2]['longitude']) },
		]

		// Construct area polygon
		const bermudaTriangle = new google.maps.Polygon({
			paths: triangleCoords,
			strokeColor: strokeColor,
			strokeOpacity: strokeOpacity,
			strokeWeight: strokeWeight,
			fillColor: fillColor,
			fillOpacity: fillOpacity,
		})

		// Set the area onto the map
		bermudaTriangle.setMap(map)
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
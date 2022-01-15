<script>
	export let name;

	/* Draw the map */
	async function initMap(){ 

		const mapCenter = { lat: 12, lng: 10 }
		const markerPos1 = { lat: 10, lng: 20 }
		const markerPos2 = { lat: 8, lng: 15 }
		const mapElement = document.getElementById('project-map')
		
		// map element
		const map = new google.maps.Map( mapElement, { zoom: 3, center: mapCenter })
			
		// marker elements
		const marker1 = new google.maps.Marker({
			position: markerPos1,
			map: map
		})
		const marker2 = new google.maps.Marker({
			position: markerPos2,
			map: map
		})

		// To add the marker to the map, call setMap();
		marker1.setMap(map)
		marker2.setMap(map)

		const triangleCoords = [
			{ lat: 25.774, lng: -80.19 },
			{ lat: 18.466, lng: -66.118 },
			{ lat: 32.321, lng: -64.757 },
			{ lat: 25.774, lng: -80.19 },
		]
		// Construct the polygon.
		const bermudaTriangle = new google.maps.Polygon({
		paths: triangleCoords,
		strokeColor: "#FF0000",
		strokeOpacity: 0.8,
		strokeWeight: 2,
		fillColor: "#FF0000",
		fillOpacity: 0.35,
		})

		bermudaTriangle.setMap(map)


		var locations = await getLatestLocations()
		console.log(await locations)

	}

	/* Getting latest locations of androids and iot device */
	async function getLatestLocations(){

		const url = 'http://localhost:4000/events'

		const response = await fetch(url, {
			method: 'GET',
			cache: 'no-cache',
			headers: { 'Content-Type': 'application/json' },
			//mode: 'no-cors',
			referrerPolicy: 'no-referrer',
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

	h1 {
		color: #ff3e00;
		text-transform: uppercase;
		font-size: 4em;
		font-weight: 100;
	}

	#project-map {
		height: 400px;
		width: 400px;
	}

	@media (min-width: 640px) {
		main {
			max-width: none;
		}
	}
</style>
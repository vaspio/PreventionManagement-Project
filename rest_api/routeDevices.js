const express = require('express')
const router = express.Router()
const mysql = require('mysql')

router.get('/', (req, res, next) => {

	var connection = mysql.createConnection({
		host     : 'localhost',
		user     : 'root',
		password : 'Project123!',
		database : 'preventiondb'
	})
	connection.connect()

	connection.query('SELECT * FROM devices WHERE device_type = "iot"', function (error, results, fields) {
		if (error){
			res.status(500).json({
				message: 'There was a problem with the database',
				error: error
			})
		} else {
			res.status(200).json({
				message: 'Devices have been fetched',
				devices: results
			})
			console.table(results)
		}
	})

	connection.end()

})

module.exports = router

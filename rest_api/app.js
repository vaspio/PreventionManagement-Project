const express = require('express')
//var cors = require('cors')

const app = express()
const eventRoutes = require('./routes')

app.use('/events', eventRoutes)

app.use(function (req, res, next) {

    //res.setHeader('Access-Control-Allow-Origin', 'null');
    res.setHeader('Access-Control-Allow-Origin', 'http://localhost:8080');
    res.setHeader('Access-Control-Allow-Methods', 'GET');
    res.setHeader('Access-Control-Allow-Headers', 'X-Requested-With,content-type');

    next();
})

//app.use(cors())
//app.use(cors({ credentials: true, origin: true }))
//app.options('*', cors());

module.exports = app

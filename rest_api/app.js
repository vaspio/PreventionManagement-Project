const express = require('express')
const cors = require('cors')

const app = express()
const eventRoutes = require('./routeEvents')
const deviceRoutes = require('./routeDevices')


const corsOptions = {
    origin: '*',
}
app.use(cors(corsOptions))
app.use(function (req, res, next) {
    next();
})
app.use('/events', eventRoutes)
app.use('/devices', deviceRoutes)

module.exports = app

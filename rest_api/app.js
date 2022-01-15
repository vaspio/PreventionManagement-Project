const express = require('express')
const cors = require('cors')

const app = express()
const eventRoutes = require('./routes')

const corsOptions = {
    origin: '*',
}
app.use(cors(corsOptions))
app.use(function (req, res, next) {
    next();
})
app.use('/events', eventRoutes)

module.exports = app

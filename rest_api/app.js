const express = require('express')
var cors = require('cors')

const app = express()
const eventRoutes = require('./routes')

/*app.use((req, res, next) => {
    res.append('Access-Control-Allow-Origin', ['*']);
    res.append('Access-Control-Allow-Methods', 'GET');
    res.append('Access-Control-Allow-Headers', 'Content-Type');
    res.append('Access-Control-Allow-Credentials', true);
    res.append('Content-Type', 'application/json');
    next();
});*/

app.use('/events', eventRoutes)
app.use(cors())
//app.use(cors({ credentials: true, origin: true }))
app.options('*', cors());

module.exports = app

import React from 'react';
import ReactDOM from 'react-dom';

import formElements from './formElements'
import CustomDatePicker from './customDatePicker'

require('react-datepicker/dist/react-datepicker.css');


export default {
        init: function() {
            ReactDOM.render(
                <CustomDatePicker />,
                document.getElementById(formElements.PAPER_CHECKOUT_DATE_PICKER_ID)
            )
        }
}

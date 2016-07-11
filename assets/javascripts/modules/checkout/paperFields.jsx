import React from 'react';
import ReactDOM from 'react-dom';

import formElements from './formElements'
import CustomDatePicker from './customDatePicker'
import CharacterCountedTextArea from './characterCountedTextArea'

require('react-datepicker/dist/react-datepicker.css');


export default {
        init: function() {
            ReactDOM.render(
                <CharacterCountedTextArea name="deliveryInstructions" maxLength="250" className="input-text js-input" rows="4"/>,
                document.getElementById(formElements.DELIVERY_INSTRUCTIONS_ID)
            );
            ReactDOM.render(
                <CustomDatePicker name="startDate" className="input-text" dateFormat="D MMMM YYYY"/>,
                document.getElementById(formElements.PAPER_CHECKOUT_DATE_PICKER_ID)
            );
        }
}

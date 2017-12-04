const SHOW_CLASS = 'animate-show-shown';
const HIDE_CLASS = 'animate-hide-hidden';
const ANIMATE_SHOW_CLASS = 'animate-show';
const ANIMATE_HIDE_CLASS = 'animate-hide';

function animationFinishHandler(element) {
    element.addEventListener('animationend', (event) => {
        event.preventDefault();
        element.classList.remove(ANIMATE_HIDE_CLASS);
        element.classList.remove(ANIMATE_SHOW_CLASS);
        if (event.animationName === 'hide') {
            element.classList.add(HIDE_CLASS)
        } else if (event.animationName === 'show') {
            element.classList.remove(HIDE_CLASS)
        }
    })
}


function show(element) {
    element.classList.remove(ANIMATE_HIDE_CLASS);
    element.classList.add(ANIMATE_SHOW_CLASS);
    element.classList.remove(HIDE_CLASS)
}

export function init() {
    let elements = [...document.querySelectorAll('.js-hide')];
    elements.map((element) => {
        let otherElements = elements.filter((e) => {
            return e !== element
        });
        let children = [...document.querySelectorAll('.js-dropdown-' + element.dataset.dropdownMenu)];
        element.addEventListener('click', () => {
            if (element.classList.contains(SHOW_CLASS)) {
                element.classList.remove(SHOW_CLASS);
                otherElements.map(show);
                children.map((e) => {
                    e.classList.add(ANIMATE_HIDE_CLASS);
                });
            } else {
                children.map(show);
                element.classList.add(SHOW_CLASS);
                otherElements.map((e) => {
                    e.classList.add(ANIMATE_HIDE_CLASS);
                })
            }
        });
        animationFinishHandler(element);
        children.map(animationFinishHandler)
    });
}

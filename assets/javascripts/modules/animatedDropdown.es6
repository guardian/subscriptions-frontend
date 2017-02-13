const SHOW_CLASS = 'js-show';


function animationFinishHandler(element) {
    element.addEventListener('animationend', (event) => {
        event.preventDefault();
        element.classList.remove('animate-hide');
        element.classList.remove('animate-show');
        if (event.animationName === 'hide') {
            element.classList.add('animate-hide-hidden')
        } else if (event.animationName === 'show') {
            element.classList.remove('animate-hide-hidden')
        }
    })
}


function show(element) {
    element.classList.remove('animate-hide');
    element.classList.add('animate-show');
    element.classList.remove('animate-hide-hidden')
}

export function init() {
    let elements = [...document.querySelectorAll('.js-hide')];
    elements.map((element) => {
        let otherElements = elements.filter((e) => {
            return e !== element
        });
        let children = [...document.querySelectorAll('.js-dropdown-' + element.dataset.dropdownMenu)];
        element.addEventListener('click', (event) => {
            if (element.classList.contains(SHOW_CLASS)) {
                element.classList.remove(SHOW_CLASS);
                otherElements.map(show);
                children.map((e) => {
                    e.classList.add('animate-hide');
                });
            } else {
                children.map(show);
                element.classList.add(SHOW_CLASS);
                otherElements.map((e) => {
                    e.classList.add('animate-hide');
                })
            }
        });
        animationFinishHandler(element);
        children.map(animationFinishHandler)
    });
}

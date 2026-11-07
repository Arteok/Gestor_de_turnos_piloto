'use strict';
const $ = (selector) => document.querySelector(selector);
const tabla = $('#tabla-turnos');
const mensaje = $('#mensaje');
const modal = $('#modal-reprogramar');
let lista = [];

function aviso(texto, error = false) {
  mensaje.textContent = texto;
  mensaje.classList.toggle('error', error);
  mensaje.hidden = false;
}

async function api(ruta, datos) {
  const respuesta = await fetch(ruta, datos ? {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
    body: new URLSearchParams(datos)
  } : undefined);
  const json = await respuesta.json();
  if (!respuesta.ok) throw new Error(json.mensaje || 'Ocurrió un error');
  return json;
}

function celda(tr, texto, clase) {
  const td = document.createElement('td');
  td.textContent = texto;
  if (clase) td.className = clase;
  tr.appendChild(td);
  return td;
}

function boton(texto, clase, funcion) {
  const b = document.createElement('button');
  b.type = 'button'; b.className = `small ${clase}`.trim(); b.textContent = texto;
  b.addEventListener('click', funcion);
  return b;
}

function render() {
  $('#total').textContent = lista.length;
  $('#activos').textContent = lista.filter(t => t.estado === 'PENDIENTE').length;
  $('#cancelados').textContent = lista.filter(t => t.estado === 'CANCELADO').length;
  tabla.replaceChildren();
  if (!lista.length) {
    const tr = document.createElement('tr'); const td = celda(tr,'Todavía no hay turnos. Creá una reserva para comenzar.','empty'); td.colSpan=5; tabla.appendChild(tr); return;
  }
  const ordenada = [...lista].sort((a,b) => `${a.fecha} ${a.hora}`.localeCompare(`${b.fecha} ${b.hora}`));
  for (const turno of ordenada) {
    const tr = document.createElement('tr');
    const paciente = celda(tr,turno.paciente);
    const tel = document.createElement('span'); tel.className = 'muted'; tel.textContent = turno.telefono;
    paciente.appendChild(document.createElement('br')); paciente.appendChild(tel);
    const servicio = celda(tr,turno.servicio);
    const dur = document.createElement('span');dur.className = 'muted';dur.textContent = `${turno.duracionMinutos} min`;
    servicio.appendChild(document.createElement('br')); servicio.appendChild(dur);
    celda(tr, `${turno.fecha} · ${turno.hora}`);
    const estado = celda(tr,'');
    const chip = document.createElement('span');chip.className = `badge ${turno.estado === 'PENDIENTE' ? 'pending' : 'cancel'}`;
    chip.textContent = turno.estado === 'PENDIENTE' ? 'Pendiente' : 'Cancelado'; estado.appendChild(chip);
    const acciones = celda(tr,'');
    if (turno.estado === 'PENDIENTE') {
      acciones.appendChild(boton('Reprogramar','',() => {
        $('#reprogramar-id').value = turno.id; $('#reprogramar-fecha').value = turno.fecha;
        $('#reprogramar-hora').value = turno.hora; modal.showModal();
      }));
      acciones.appendChild(boton('Cancelar','danger',async () => {
        if (!confirm(`¿Cancelar el turno de ${turno.paciente}?`)) return;
        try { const r = await api('/api/turnos/cancelar',{id:String(turno.id)}); aviso(r.mensaje); await cargarTurnos(); }
        catch (e) { aviso(e.message,true); }
      }));
    } else acciones.textContent = '—';
    tabla.appendChild(tr);
  }
}

async function cargarTurnos() {
  lista = (await api('/api/turnos')).turnos;
  render();
}

async function iniciar() {
  try {
    const catalogo = await api('/api/catalogo');
    const selector = $('#servicioId'); selector.replaceChildren();
    for (const item of catalogo.servicios) {
      const op = document.createElement('option');op.value = item.id;
      op.textContent = `${item.nombre} (${item.duracionMinutos} min)`;
      selector.appendChild(op);
    }
    await cargarTurnos();
  } catch (e) { aviso(`No se pudo conectar con el servidor: ${e.message}`,true); }
}

$('#form-reserva').addEventListener('submit',async (evento) => {
  evento.preventDefault();
  const boton = $('#boton-crear');boton.disabled = true;
  try {
    const resultado = await api('/api/turnos',Object.fromEntries(new FormData(evento.target).entries()));
    aviso(`${resultado.mensaje} · Nº ${resultado.id}`);
    evento.target.reset();
    await cargarTurnos();
  } catch (e) { aviso(e.message,true); }
  finally { boton.disabled = false; }
});

$('#cerrar-modal').addEventListener('click',() => modal.close());
$('#form-reprogramar').addEventListener('submit',async (evento) => {
  evento.preventDefault();
  try {
    const resultado = await api('/api/turnos/reprogramar',Object.fromEntries(new FormData(evento.target).entries()));
    modal.close(); aviso(resultado.mensaje); await cargarTurnos();
  } catch (e) { aviso(e.message,true); }
});
iniciar();
